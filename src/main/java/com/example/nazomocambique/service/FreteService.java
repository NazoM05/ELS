package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.nazomocambique.config.CorreiosProperties;
import com.example.nazomocambique.model.Cotacao;
import com.example.nazomocambique.model.OpcaoFrete;
import com.example.nazomocambique.model.Pacote;

/**
 * Monta as opções de entrega do pedido.
 *
 * - Correios configurados: uma opção por serviço (PAC, SEDEX...) com preço e prazo reais.
 * - Correios desligados ou fora do ar: uma opção "Entrega padrão" com a regra antiga da loja
 *   (frete fixo + prazo por estado), então o checkout nunca fica sem frete.
 * - Frete grátis (R$ 99 no Sul, R$ 250 nas demais regiões) vale para a opção mais barata.
 */
@Service
public class FreteService {

	public static final String CODIGO_PADRAO = "PADRAO";

	private static final Logger log = LoggerFactory.getLogger(FreteService.class);
	private static final Map<String, String> NOMES = Map.of("03298", "PAC", "03220", "SEDEX");

	private final CorreiosProperties props;
	private final CotacaoCorreios correios;
	private final PrazoEntregaService prazos;

	public FreteService(CorreiosProperties props, CotacaoCorreios correios, PrazoEntregaService prazos) {
		this.props = props;
		this.correios = correios;
		this.prazos = prazos;
	}

	/** Opções ordenadas da mais barata para a mais cara. Vazia quando ainda falta o estado. */
	public List<OpcaoFrete> cotar(String cep, String uf, BigDecimal subtotal, Pacote pacote) {
		String cepNumeros = cep == null ? "" : cep.replaceAll("\\D", "");
		if (props.configurado() && cepNumeros.length() == 8 && subtotal.signum() > 0) {
			List<OpcaoFrete> opcoes = consultarCorreios(cepNumeros, pacote);
			if (!opcoes.isEmpty()) {
				return aplicarFreteGratis(opcoes, subtotal, uf);
			}
		}
		return opcaoPadrao(subtotal, uf).map(List::of).orElse(List.of());
	}

	/** A opção escolhida pelo código; sem código (ou código desconhecido) vale a mais barata. */
	public static Optional<OpcaoFrete> escolher(List<OpcaoFrete> opcoes, String codigo) {
		if (opcoes == null || opcoes.isEmpty()) {
			return Optional.empty();
		}
		String procurado = codigo == null ? "" : codigo.trim();
		return opcoes.stream().filter(o -> o.codigo().equals(procurado)).findFirst()
				.or(() -> Optional.of(opcoes.get(0)));
	}

	// ---------------------------------------------------------------- apoio

	private List<OpcaoFrete> consultarCorreios(String cepDestino, Pacote pacote) {
		List<OpcaoFrete> opcoes = new ArrayList<>();
		for (String codigo : props.servicos()) {
			try {
				Cotacao c = correios.cotar(codigo, cepDestino, pacote);
				opcoes.add(new OpcaoFrete(codigo, NOMES.getOrDefault(codigo, "Correios " + codigo), c.valor(),
						c.prazoDias(), "CORREIOS", false));
			} catch (CorreiosIndisponivelException e) {
				log.warn("Sem cotação dos Correios para o serviço {}: {}", codigo, e.getMessage());
			}
		}
		opcoes.sort(Comparator.comparing(OpcaoFrete::valor).thenComparingInt(OpcaoFrete::prazoDias));
		return opcoes;
	}

	private List<OpcaoFrete> aplicarFreteGratis(List<OpcaoFrete> opcoes, BigDecimal subtotal, String uf) {
		BigDecimal minimo = PedidoService.minimoFreteGratis(uf);
		if (minimo == null || subtotal.compareTo(minimo) < 0) {
			return opcoes;
		}
		List<OpcaoFrete> resultado = new ArrayList<>(opcoes);
		resultado.set(0, resultado.get(0).comValorGratis()); // a mais barata fica por conta da loja
		return resultado;
	}

	private Optional<OpcaoFrete> opcaoPadrao(BigDecimal subtotal, String uf) {
		BigDecimal valor = PedidoService.calcularFrete(subtotal, uf);
		if (valor == null) {
			return Optional.empty(); // estado ainda não informado
		}
		int dias = prazos.buscar(uf).map(p -> p.diasMax()).orElse(10);
		return Optional.of(new OpcaoFrete(CODIGO_PADRAO, "Entrega padrão", valor, dias, "LOJA", valor.signum() == 0));
	}
}
