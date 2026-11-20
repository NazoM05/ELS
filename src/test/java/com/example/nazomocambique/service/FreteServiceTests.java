package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.nazomocambique.config.CorreiosProperties;
import com.example.nazomocambique.model.Cotacao;
import com.example.nazomocambique.model.OpcaoFrete;
import com.example.nazomocambique.model.Pacote;

class FreteServiceTests {

	private static final Pacote PACOTE = new Pacote(500, 30, 22, 5);
	private static final String CEP = "90010-000";

	private final PrazoEntregaService prazos = new PrazoEntregaService();

	private static CorreiosProperties props(boolean ligado) {
		return new CorreiosProperties(ligado, "", "12345678000199", "codigo", "0070123456", "90010000",
				List.of("03298", "03220"), null);
	}

	/** PAC (03298) R$ 32,50 em 6 dias; SEDEX (03220) R$ 48,90 em 2 dias. */
	private static final CotacaoCorreios TABELA = (codigo, cep, pacote) -> "03298".equals(codigo)
			? new Cotacao(new BigDecimal("32.50"), 6)
			: new Cotacao(new BigDecimal("48.90"), 2);

	private static final CotacaoCorreios FORA_DO_AR = (codigo, cep, pacote) -> {
		throw new CorreiosIndisponivelException("fora do ar");
	};

	private FreteService servico(boolean ligado, CotacaoCorreios correios) {
		return new FreteService(props(ligado), correios, prazos);
	}

	@Test
	void correiosDesligadoUsaFreteFixoEPrazoDoEstado() {
		List<OpcaoFrete> opcoes = servico(false, TABELA).cotar(CEP, "RJ", new BigDecimal("80.00"), PACOTE);
		assertEquals(1, opcoes.size());
		OpcaoFrete o = opcoes.get(0);
		assertEquals(FreteService.CODIGO_PADRAO, o.codigo());
		assertEquals("LOJA", o.origem());
		assertEquals(new BigDecimal("19.90"), o.valor());
		assertEquals(6, o.prazoDias()); // RJ: 4 a 6 dias úteis
	}

	@Test
	void semEstadoNaoHaOpcaoFixa() {
		assertTrue(servico(false, TABELA).cotar(CEP, "", new BigDecimal("80.00"), PACOTE).isEmpty());
	}

	@Test
	void correiosLigadoDevolveUmaOpcaoPorServicoDaMaisBarataParaMaisCara() {
		List<OpcaoFrete> opcoes = servico(true, TABELA).cotar(CEP, "RJ", new BigDecimal("80.00"), PACOTE);
		assertEquals(2, opcoes.size());
		assertEquals("PAC", opcoes.get(0).nome());
		assertEquals(new BigDecimal("32.50"), opcoes.get(0).valor());
		assertEquals("SEDEX", opcoes.get(1).nome());
		assertEquals(2, opcoes.get(1).prazoDias());
		assertEquals("CORREIOS", opcoes.get(0).origem());
	}

	@Test
	void freteGratisCobreSoAOpcaoMaisBarata() {
		// RS: grátis a partir de R$ 99
		List<OpcaoFrete> opcoes = servico(true, TABELA).cotar(CEP, "RS", new BigDecimal("120.00"), PACOTE);
		assertTrue(opcoes.get(0).gratis());
		assertEquals(0, opcoes.get(0).valor().signum());
		assertFalse(opcoes.get(1).gratis());
		assertEquals(new BigDecimal("48.90"), opcoes.get(1).valor());
	}

	@Test
	void foraDoSulOMinimoParaFreteGratisE250() {
		List<OpcaoFrete> opcoes = servico(true, TABELA).cotar(CEP, "RJ", new BigDecimal("120.00"), PACOTE);
		assertFalse(opcoes.get(0).gratis());
		List<OpcaoFrete> acima = servico(true, TABELA).cotar(CEP, "RJ", new BigDecimal("250.00"), PACOTE);
		assertTrue(acima.get(0).gratis());
	}

	@Test
	void seOsCorreiosFalhamVoltaParaOFreteFixo() {
		List<OpcaoFrete> opcoes = servico(true, FORA_DO_AR).cotar(CEP, "SP", new BigDecimal("80.00"), PACOTE);
		assertEquals(1, opcoes.size());
		assertEquals(FreteService.CODIGO_PADRAO, opcoes.get(0).codigo());
		assertEquals(new BigDecimal("19.90"), opcoes.get(0).valor());
	}

	@Test
	void seSoUmServicoFalhaOutroContinuaDisponivel() {
		CotacaoCorreios soPac = (codigo, cep, pacote) -> {
			if ("03220".equals(codigo)) {
				throw new CorreiosIndisponivelException("sedex indisponível");
			}
			return new Cotacao(new BigDecimal("32.50"), 6);
		};
		List<OpcaoFrete> opcoes = servico(true, soPac).cotar(CEP, "SP", new BigDecimal("80.00"), PACOTE);
		assertEquals(1, opcoes.size());
		assertEquals("PAC", opcoes.get(0).nome());
	}

	@Test
	void cepIncompletoNaoConsultaOsCorreios() {
		List<OpcaoFrete> opcoes = servico(true, TABELA).cotar("9001", "SP", new BigDecimal("80.00"), PACOTE);
		assertEquals(FreteService.CODIGO_PADRAO, opcoes.get(0).codigo());
	}

	@Test
	void escolherPeloCodigoOuUsarAMaisBarata() {
		List<OpcaoFrete> opcoes = servico(true, TABELA).cotar(CEP, "SP", new BigDecimal("80.00"), PACOTE);
		assertEquals("SEDEX", FreteService.escolher(opcoes, "03220").orElseThrow().nome());
		assertEquals("PAC", FreteService.escolher(opcoes, "").orElseThrow().nome());
		assertEquals("PAC", FreteService.escolher(opcoes, "999").orElseThrow().nome());
		assertTrue(FreteService.escolher(List.of(), "03220").isEmpty());
	}

	@Test
	void propriedadesIncompletasDesligamOsCorreios() {
		CorreiosProperties semCartao = new CorreiosProperties(true, "", "123", "codigo", "", "90010000", null, null);
		assertFalse(semCartao.configurado());
		assertTrue(props(true).configurado());
		assertFalse(props(false).configurado());
	}
}
