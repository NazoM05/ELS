package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CodigoPedidoTests {

	@Test
	void codigoTemPrefixoDataDaCompraEQuatroCaracteres() {
		String codigo = CodigoPedido.gerar(LocalDate.of(2026, 11, 20), new Random(1));
		assertTrue(codigo.startsWith("EL-261120-"), codigo);
		assertEquals(14, codigo.length());
		assertTrue(CodigoPedido.valido(codigo));
	}

	@Test
	void codigoNaoUsaCaracteresQueSeConfundem() {
		Random r = new Random(42);
		for (int i = 0; i < 2000; i++) {
			String sufixo = CodigoPedido.gerar(LocalDate.of(2026, 1, 1), r).substring(10);
			assertFalse(sufixo.matches(".*[01OIL].*"), sufixo);
		}
	}

	@Test
	void codigosSorteadosSaoDiferentes() {
		Set<String> vistos = new HashSet<>();
		for (int i = 0; i < 500; i++) {
			vistos.add(CodigoPedido.gerar(LocalDate.of(2026, 11, 20)));
		}
		assertTrue(vistos.size() >= 495, "repetiu demais: " + vistos.size());
	}

	@Test
	void codigoDigitadoSemHifenOuMinusculoEncontraOMesmoPedido() {
		assertEquals("EL-261120-7K3Q", CodigoPedido.normalizar(" el 261120 7k3q "));
		assertEquals("EL-261120-7K3Q", CodigoPedido.normalizar("el-261120-7k3q"));
	}
}
