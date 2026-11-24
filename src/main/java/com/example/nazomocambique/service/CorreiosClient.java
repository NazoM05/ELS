package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.nazomocambique.config.CorreiosProperties;
import com.example.nazomocambique.model.Cotacao;
import com.example.nazomocambique.model.Pacote;
import com.example.nazomocambique.model.PrePostagem;
import com.example.nazomocambique.model.SolicitacaoPostagem;

/**
 * Cliente da API REST dos Correios (CWS): gera o token com o cartão de postagem e consulta preço e prazo.
 *
 * Fluxo: POST /token/v1/autentica/cartaopostagem (Basic Auth: CNPJ + código de acesso do portal CWS)
 * -> GET /preco/v1/nacional/{serviço} e GET /prazo/v1/nacional/{serviço} com "Bearer token".
 * As APIs de preço (serviço 38202) e prazo (38210) precisam estar liberadas no contrato.
 * Confira nome de campos e versão dos endpoints no manual do CWS se algo vier diferente.
 *
 * Pré-postagem (etiqueta): POST /prepostagem/v1/prepostagens com o remetente, o destinatário, o serviço, o peso, as
 * medidas e a declaração de conteúdo. A resposta traz o código do objeto (rastreamento), que a loja só repassa.
 * ATENÇÃO: os nomes e tipos dos campos do corpo (montarCorpoPrePostagem) vêm da documentação pública e não foram
 * testados contra o ambiente real; valide no Swagger do CWS antes de usar em produção.
 */
@Service
public class CorreiosClient implements CotacaoCorreios, PostagemCorreios {

	private static final Logger log = LoggerFactory.getLogger(CorreiosClient.class);
	private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
	};
	/** Token: renova um pouco antes de vencer. Se a API não informar a validade, assume este tempo. */
	private static final Duration MARGEM_TOKEN = Duration.ofSeconds(60);
	private static final Duration VALIDADE_PADRAO_TOKEN = Duration.ofMinutes(25);
	/** Cotação repetida (mesmo destino, peso e medidas) é reaproveitada por este tempo. */
	private static final Duration VALIDADE_COTACAO = Duration.ofMinutes(10);
	private static final int MAX_COTACOES_EM_CACHE = 500;

	private record Guardada(Cotacao cotacao, Instant expira) {
	}

	private final CorreiosProperties props;
	private final RestClient http;
	private final Map<String, Guardada> cache = new ConcurrentHashMap<>();
	private String token;
	private Instant tokenExpira = Instant.EPOCH;

	public CorreiosClient(CorreiosProperties props) {
		this.props = props;
		HttpClient jdk = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(jdk);
		fabrica.setReadTimeout(props.timeout());
		this.http = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(fabrica).build();
	}

	@Override
	public Cotacao cotar(String codigoServico, String cepDestino, Pacote pacote) {
		String chave = codigoServico + "|" + cepDestino + "|" + pacote;
		Guardada guardada = cache.get(chave);
		if (guardada != null && Instant.now().isBefore(guardada.expira())) {
			return guardada.cotacao();
		}
		try {
			BigDecimal valor = consultarPreco(codigoServico, cepDestino, pacote);
			int prazo = consultarPrazo(codigoServico, cepDestino);
			Cotacao cotacao = new Cotacao(valor, prazo);
			if (cache.size() >= MAX_COTACOES_EM_CACHE) {
				cache.clear();
			}
			cache.put(chave, new Guardada(cotacao, Instant.now().plus(VALIDADE_COTACAO)));
			return cotacao;
		} catch (CorreiosIndisponivelException e) {
			throw e;
		} catch (RestClientException | IllegalArgumentException e) {
			throw new CorreiosIndisponivelException("Falha ao consultar os Correios (serviço " + codigoServico + ")", e);
		}
	}

	// ---------------------------------------------------------------- pré-postagem

	@Override
	public PrePostagem criarPrePostagem(SolicitacaoPostagem solicitacao) {
		Map<String, Object> corpo = montarCorpoPrePostagem(props.cepOrigem(), props.remetente(), solicitacao);
		try {
			Map<String, Object> resp = comToken(t -> http.post()
					.uri("/prepostagem/v1/prepostagens")
					.headers(h -> h.setBearerAuth(t))
					.contentType(MediaType.APPLICATION_JSON)
					.body(corpo)
					.retrieve().body(MAPA));
			String id = texto(resp, "id");
			String codigoObjeto = texto(resp, "codigoObjeto");
			if (id.isEmpty() || codigoObjeto.isEmpty()) {
				throw new CorreiosIndisponivelException("A resposta dos Correios não trouxe o id e o código do objeto.");
			}
			log.info("Pré-postagem {} criada (objeto {}).", id, codigoObjeto);
			return new PrePostagem(id, codigoObjeto.toUpperCase(java.util.Locale.ROOT));
		} catch (CorreiosIndisponivelException e) {
			throw e;
		} catch (HttpClientErrorException e) {
			String detalhe = e.getResponseBodyAsString();
			throw new CorreiosIndisponivelException("Os Correios recusaram a pré-postagem (HTTP " + e.getStatusCode().value()
					+ "): " + (detalhe.length() > 300 ? detalhe.substring(0, 300) : detalhe), e);
		} catch (RestClientException e) {
			throw new CorreiosIndisponivelException("Falha ao criar a pré-postagem nos Correios.", e);
		}
	}

	/** Corpo do POST de pré-postagem. Estático e sem rede para poder ser conferido nos testes. */
	static Map<String, Object> montarCorpoPrePostagem(String cepOrigem, CorreiosProperties.Remetente r,
			SolicitacaoPostagem s) {
		Map<String, Object> remetente = new LinkedHashMap<>();
		remetente.put("nome", r.nome());
		remetente.put("cpfCnpj", r.cpfCnpj());
		if (!r.email().isEmpty()) {
			remetente.put("email", r.email());
		}
		String telR = r.telefone();
		if (telR.length() >= 10) {
			remetente.put("dddTelefone", telR.substring(0, 2));
			remetente.put("telefone", telR.substring(2));
		}
		remetente.put("endereco", endereco(cepOrigem, r.logradouro(), r.numero(), r.complemento(), r.bairro(), r.cidade(), r.uf()));

		SolicitacaoPostagem.Destino d = s.destino();
		Map<String, Object> destinatario = new LinkedHashMap<>();
		destinatario.put("nome", d.nome());
		if (!d.telefone().isEmpty()) {
			destinatario.put("dddTelefone", d.ddd());
			destinatario.put("telefone", d.telefone());
		}
		destinatario.put("endereco", endereco(d.cep(), d.logradouro(), d.numero(), d.complemento(), d.bairro(), d.cidade(), d.uf()));

		List<Map<String, Object>> itens = new ArrayList<>();
		for (SolicitacaoPostagem.Item i : s.itens()) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("conteudo", i.conteudo().length() > 60 ? i.conteudo().substring(0, 60) : i.conteudo());
			item.put("quantidade", String.valueOf(i.quantidade()));
			item.put("valor", i.valor().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
			itens.add(item);
		}

		Pacote p = s.pacote();
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("remetente", remetente);
		corpo.put("destinatario", destinatario);
		corpo.put("codigoServico", s.codigoServico());
		corpo.put("pesoInformado", String.valueOf(p.pesoGramas()));
		corpo.put("codigoFormatoObjetoInformado", "2"); // 2 = caixa/pacote (o mesmo tpObjeto da cotação)
		corpo.put("comprimentoInformado", String.valueOf(p.comprimentoCm()));
		corpo.put("larguraInformada", String.valueOf(p.larguraCm()));
		corpo.put("alturaInformada", String.valueOf(p.alturaCm()));
		corpo.put("cienteObjetoNaoProibido", 1);
		corpo.put("itensDeclaracaoConteudo", itens);
		return corpo;
	}

	private static Map<String, Object> endereco(String cep, String logradouro, String numero, String complemento,
			String bairro, String cidade, String uf) {
		Map<String, Object> e = new LinkedHashMap<>();
		e.put("cep", cep);
		e.put("logradouro", logradouro);
		e.put("numero", numero);
		if (complemento != null && !complemento.isBlank()) {
			e.put("complemento", complemento);
		}
		e.put("bairro", bairro);
		e.put("cidade", cidade);
		e.put("uf", uf);
		return e;
	}

	private static String texto(Map<String, Object> mapa, String chave) {
		Object v = mapa == null ? null : mapa.get(chave);
		return v == null ? "" : v.toString().trim();
	}

	// ---------------------------------------------------------------- chamadas

	private BigDecimal consultarPreco(String servico, String cepDestino, Pacote p) {
		Map<String, Object> resp = comToken(t -> http.get()
				.uri(u -> u.path("/preco/v1/nacional/{servico}")
						.queryParam("cepOrigem", props.cepOrigem())
						.queryParam("cepDestino", cepDestino)
						.queryParam("psObjeto", p.pesoGramas()) // gramas
						.queryParam("tpObjeto", 2) // 2 = pacote
						.queryParam("comprimento", p.comprimentoCm())
						.queryParam("largura", p.larguraCm())
						.queryParam("altura", p.alturaCm())
						.build(servico))
				.headers(h -> h.setBearerAuth(t))
				.retrieve().body(MAPA));
		return lerValor(resp == null ? null : resp.get("pcFinal"));
	}

	private int consultarPrazo(String servico, String cepDestino) {
		Map<String, Object> resp = comToken(t -> http.get()
				.uri(u -> u.path("/prazo/v1/nacional/{servico}")
						.queryParam("cepOrigem", props.cepOrigem())
						.queryParam("cepDestino", cepDestino)
						.build(servico))
				.headers(h -> h.setBearerAuth(t))
				.retrieve().body(MAPA));
		Object dias = resp == null ? null : resp.get("prazoEntrega");
		if (dias instanceof Number n) {
			return Math.max(n.intValue(), 1);
		}
		if (dias != null) {
			return Math.max(Integer.parseInt(dias.toString().trim()), 1);
		}
		throw new CorreiosIndisponivelException("Resposta de prazo sem o campo prazoEntrega.");
	}

	// ---------------------------------------------------------------- token

	/** Executa a chamada com o token atual; se vier 401 (token vencido/revogado), gera outro e tenta uma vez. */
	private <T> T comToken(java.util.function.Function<String, T> chamada) {
		try {
			return chamada.apply(token());
		} catch (HttpClientErrorException.Unauthorized e) {
			limparToken();
			return chamada.apply(token());
		}
	}

	private synchronized String token() {
		if (token != null && Instant.now().isBefore(tokenExpira.minus(MARGEM_TOKEN))) {
			return token;
		}
		Map<String, Object> resp = http.post()
				.uri("/token/v1/autentica/cartaopostagem")
				.headers(h -> h.setBasicAuth(props.usuario(), props.codigoAcesso()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("numero", props.cartaoPostagem()))
				.retrieve().body(MAPA);
		Object novo = resp == null ? null : resp.get("token");
		if (novo == null || novo.toString().isBlank()) {
			throw new CorreiosIndisponivelException("A API dos Correios não devolveu o token.");
		}
		token = novo.toString();
		tokenExpira = lerExpiracao(resp.get("expiraEm"));
		log.info("Token dos Correios renovado.");
		return token;
	}

	private synchronized void limparToken() {
		token = null;
		tokenExpira = Instant.EPOCH;
	}

	// ---------------------------------------------------------------- leitura dos campos

	/** "25,90" (vírgula decimal), "1.025,90" ou 25.9 (número) viram BigDecimal. */
	static BigDecimal lerValor(Object bruto) {
		if (bruto == null) {
			throw new CorreiosIndisponivelException("Resposta de preço sem o campo pcFinal.");
		}
		if (bruto instanceof Number n) {
			return new BigDecimal(n.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
		}
		String texto = bruto.toString().trim();
		if (texto.contains(",")) {
			texto = texto.replace(".", "").replace(',', '.');
		}
		return new BigDecimal(texto).setScale(2, java.math.RoundingMode.HALF_UP);
	}

	static Instant lerExpiracao(Object bruto) {
		Instant padrao = Instant.now().plus(VALIDADE_PADRAO_TOKEN);
		if (bruto == null) {
			return padrao;
		}
		String texto = bruto.toString().trim();
		try {
			return OffsetDateTime.parse(texto).toInstant();
		} catch (RuntimeException e) {
			// sem fuso: a API usa o horário de Brasília
		}
		try {
			return LocalDateTime.parse(texto).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
		} catch (RuntimeException e) {
			return padrao;
		}
	}
}
