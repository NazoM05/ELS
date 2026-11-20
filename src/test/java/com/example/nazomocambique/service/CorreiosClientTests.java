package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

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
}
