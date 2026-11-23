package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class PrazoEntregaServiceTests {

	private final PrazoEntregaService service = new PrazoEntregaService();

	@Test
	void temPrazoParaOs27Estados() {
		Set<String> ufs = service.listar().stream().map(p -> p.uf()).collect(Collectors.toSet());
		assertEquals(27, ufs.size());
		assertTrue(ufs.containsAll(Set.of("AC", "DF", "RS", "RR", "SP", "TO")));
	}

	@Test
	void prazoDoParana() {
		var pr = service.buscar("pr").orElseThrow();
		assertEquals(2, pr.diasMin());
		assertEquals(4, pr.diasMax());
		assertEquals("2 a 4 dias úteis", pr.getDescricao());
	}

	@Test
	void prazoDeRoraima() {
		var rr = service.buscar("RR").orElseThrow();
		assertEquals(11, rr.diasMin());
		assertEquals(15, rr.diasMax());
	}

	@Test
	void ufDesconhecidaNaoTemPrazo() {
		assertTrue(service.buscar("XX").isEmpty());
		assertTrue(service.buscar(null).isEmpty());
	}
}
