package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.nazomocambique.config.MercadoPagoProperties;

/**
 * Cliente da API REST do Mercado Pago: cria o pagamento Pix (POST /v1/payments) e consulta um pagamento
 * (GET /v1/payments/{id}). O Access Token vai só no cabeçalho Authorization e nunca é escrito no log.
 */
@Service
public class MercadoPagoClient implements GatewayPagamento {

	private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);
	private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
	};
	/** Formato de data que o Mercado Pago espera: 2026-11-20T15:30:00.000-03:00 */
	private static final DateTimeFormatter DATA_MP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");
	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	private final MercadoPagoProperties props;
	private final RestClient http;

	public MercadoPagoClient(MercadoPagoProperties props) {
		this.props = props;
		HttpClient jdk = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(jdk);
		fabrica.setReadTimeout(props.timeout());
		this.http = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(fabrica).build();
	}

	@Override
	public Pix criarPix(SolicitacaoPix s) {
		Map<String, Object> pagador = new LinkedHashMap<>();
		pagador.put("email", s.email());
		String nome = s.nome() == null ? "" : s.nome().trim();
		if (!nome.isEmpty()) {
			int espaco = nome.indexOf(' ');
			pagador.put("first_name", espaco > 0 ? nome.substring(0, espaco) : nome);
			if (espaco > 0) {
				pagador.put("last_name", nome.substring(espaco + 1).trim());
			}
		}
		if (s.cpf() != null && s.cpf().length() == 11) {
			pagador.put("identification", Map.of("type", "CPF", "number", s.cpf()));
		}

		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("transaction_amount", s.valor().setScale(2, RoundingMode.HALF_UP));
		corpo.put("description", s.descricao());
		corpo.put("payment_method_id", "pix");
		corpo.put("external_reference", s.referencia());
		corpo.put("date_of_expiration", OffsetDateTime.now(SAO_PAULO).plusMinutes(s.minutosValidade()).format(DATA_MP));
		if (!props.notificationUrl().isEmpty()) {
			corpo.put("notification_url", props.notificationUrl());
		}
		corpo.put("payer", pagador);

		Map<String, Object> resp;
		try {
			resp = http.post().uri("/v1/payments")
					.headers(h -> {
						h.setBearerAuth(props.accessToken());
						h.set("X-Idempotency-Key", s.chaveIdempotencia());
					})
					.contentType(MediaType.APPLICATION_JSON)
					.body(corpo)
					.retrieve().body(MAPA);
		} catch (RestClientResponseException e) {
			log.warn("Mercado Pago recusou o Pix do pedido {}: HTTP {} - {}", s.referencia(), e.getStatusCode().value(),
					resumo(e.getResponseBodyAsString()));
			throw new PagamentoIndisponivelException("Não foi possível gerar o Pix agora. Tente novamente em instantes.", e);
		} catch (RestClientException e) {
			log.warn("Falha de rede ao gerar o Pix do pedido {}: {}", s.referencia(), e.getMessage());
			throw new PagamentoIndisponivelException("Não foi possível gerar o Pix agora. Tente novamente em instantes.", e);
		}

		Map<String, Object> dados = mapa(mapa(resp == null ? null : resp.get("point_of_interaction")).get("transaction_data"));
		String copiaECola = texto(dados.get("qr_code"));
		if (resp == null || !(resp.get("id") instanceof Number id) || copiaECola.isEmpty()) {
			throw new PagamentoIndisponivelException("O Mercado Pago não devolveu os dados do Pix.");
		}
		OffsetDateTime expira = lerData(resp.get("date_of_expiration"), s.minutosValidade());
		return new Pix(id.longValue(), texto(resp.get("status")), copiaECola, texto(dados.get("qr_code_base64")), expira);
	}

	@Override
	public Optional<Pagamento> consultar(long paymentId) {
		Map<String, Object> resp;
		try {
			resp = http.get().uri("/v1/payments/{id}", paymentId)
					.headers(h -> h.setBearerAuth(props.accessToken()))
					.retrieve().body(MAPA);
		} catch (HttpClientErrorException.NotFound e) {
			return Optional.empty();
		} catch (RestClientException e) {
			throw new PagamentoIndisponivelException("Falha ao consultar o pagamento " + paymentId + " no Mercado Pago.", e);
		}
		if (resp == null || !(resp.get("id") instanceof Number id)) {
			return Optional.empty();
		}
		BigDecimal valor = resp.get("transaction_amount") instanceof Number n
				? new BigDecimal(n.toString()).setScale(2, RoundingMode.HALF_UP)
				: null;
		return Optional.of(new Pagamento(id.longValue(), texto(resp.get("status")), texto(resp.get("external_reference")), valor));
	}

	// ---------------------------------------------------------------- leitura dos campos

	@SuppressWarnings("unchecked")
	private static Map<String, Object> mapa(Object o) {
		return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
	}

	private static String texto(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static OffsetDateTime lerData(Object bruto, int minutosPadrao) {
		try {
			return OffsetDateTime.parse(texto(bruto));
		} catch (RuntimeException e) {
			return OffsetDateTime.now(SAO_PAULO).plusMinutes(minutosPadrao);
		}
	}

	/** Corpo de erro curto para o log (sem despejar respostas enormes). */
	private static String resumo(String corpo) {
		if (corpo == null) {
			return "";
		}
		return corpo.length() > 400 ? corpo.substring(0, 400) + "..." : corpo;
	}
}
