package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.nazomocambique.config.CorreiosProperties;
import com.example.nazomocambique.model.Pacote;
import com.example.nazomocambique.model.SolicitacaoPostagem;

import org.junit.jupiter.api.Test;

/** Leitura dos campos que a API dos Correios devolve (sem chamar a rede). */
class CorreiosClientTests {

	@Test
	void valorComVirgulaDecimal() {
		assertEquals(new BigDecimal("25.90"), CorreiosClient.lerValor("25,90"));
		assertEquals(new BigDecimal("1025.90"), CorreiosClient.lerValor("1.025,90"));
	}

	@Test
	void valorComPontoOuNumero() {
		assertEquals(new BigDecimal("25.90"), CorreiosClient.lerValor("25.9"));
		assertEquals(new BigDecimal("25.90"), CorreiosClient.lerValor(25.9));
	}

	@Test
	void valorAusenteEErro() {
		assertThrows(CorreiosIndisponivelException.class, () -> CorreiosClient.lerValor(null));
	}

	@Test
	void expiracaoDoToken() {
		assertTrue(CorreiosClient.lerExpiracao(null).isAfter(Instant.now()));
		assertEquals(Instant.parse("2026-11-19T15:00:00Z"), CorreiosClient.lerExpiracao("2026-11-19T15:00:00Z"));
		// sem fuso: horário de Brasília (UTC-3)
		assertEquals(Instant.parse("2026-11-19T15:00:00Z"), CorreiosClient.lerExpiracao("2026-11-19T12:00:00"));
		assertTrue(CorreiosClient.lerExpiracao("lixo").isAfter(Instant.now()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void corpoDaPrePostagemTrazSoDadosReaisDoPedido() {
		CorreiosProperties.Remetente r = new CorreiosProperties.Remetente("Loja Teste", "12.345.678/0001-99", "(51) 3333-4444",
				"loja@teste.com", "Rua da Loja", "10", "", "Centro", "Porto Alegre", "rs");
		SolicitacaoPostagem s = new SolicitacaoPostagem("03298",
				new SolicitacaoPostagem.Destino("Maria", "51", "999998888", "01310100", "Av. Paulista", "1000", "Apto 4",
						"Bela Vista", "São Paulo", "SP"),
				new Pacote(500, 30, 22, 6),
				List.of(new SolicitacaoPostagem.Item("Camiseta (M)", 2, new BigDecimal("50"))));

		Map<String, Object> corpo = CorreiosClient.montarCorpoPrePostagem("90010000", r, s);

		assertEquals("03298", corpo.get("codigoServico"));
		assertEquals("500", corpo.get("pesoInformado"));
		Map<String, Object> rem = (Map<String, Object>) corpo.get("remetente");
		assertEquals("12345678000199", rem.get("cpfCnpj"));
		assertEquals("51", rem.get("dddTelefone"));
		assertEquals("33334444", rem.get("telefone"));
		assertEquals("RS", ((Map<String, Object>) rem.get("endereco")).get("uf"));
		assertEquals("90010000", ((Map<String, Object>) rem.get("endereco")).get("cep"));
		Map<String, Object> dest = (Map<String, Object>) corpo.get("destinatario");
		assertEquals("01310100", ((Map<String, Object>) dest.get("endereco")).get("cep"));
		List<Map<String, Object>> itens = (List<Map<String, Object>>) corpo.get("itensDeclaracaoConteudo");
		assertEquals("50.00", itens.get(0).get("valor"));
		assertEquals("2", itens.get(0).get("quantidade"));
		// nada de código de rastreamento no que a loja envia: ele só existe na resposta dos Correios
		assertFalse(corpo.containsKey("codigoObjeto"));
	}
}
