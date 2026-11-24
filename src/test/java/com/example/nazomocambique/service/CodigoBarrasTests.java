package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Código de barras da etiqueta: só desenha dados no formato real dos Correios. */
class CodigoBarrasTests {

	@Test
	void codigoDeObjetoValidoGeraSvg() {
		String svg = CodigoBarras.svgCodigoObjeto("AA123456789BR");
		assertTrue(svg.startsWith("<svg"));
		assertTrue(svg.contains("<path d=\"M"));
		assertTrue(svg.contains("AA123456789BR"));
	}

	@Test
	void codigoForaDoPadraoNaoGeraBarras() {
		assertEquals("", CodigoBarras.svgCodigoObjeto(null));
		assertEquals("", CodigoBarras.svgCodigoObjeto(""));
		assertEquals("", CodigoBarras.svgCodigoObjeto("aa123456789br"));
		assertEquals("", CodigoBarras.svgCodigoObjeto("AA12345678BR"));
		assertEquals("", CodigoBarras.svgCodigoObjeto("<script>"));
	}

	@Test
	void cepComOitoDigitosGeraSvg() {
		assertTrue(CodigoBarras.svgCep("90010000").startsWith("<svg"));
		assertEquals("", CodigoBarras.svgCep("90010-000"));
		assertEquals("", CodigoBarras.svgCep("9001000"));
		assertEquals("", CodigoBarras.svgCep(null));
	}

	@Test
	void mesmoCodigoGeraSempreAsMesmasBarras() {
		assertEquals(CodigoBarras.svgCep("90010000"), CodigoBarras.svgCep("90010000"));
		assertFalse(CodigoBarras.svgCep("90010000").equals(CodigoBarras.svgCep("90010001")));
	}
}
