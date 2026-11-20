package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A assinatura do webhook é o que impede qualquer um de chamar /webhooks/mercadopago. */
class MercadoPagoAssinaturaTests {

	private static final String SEGREDO = "segredo";
	private static final String REQUISICAO = "bb56a2f1-6aae-46ac-982e-9dcd3581d08e";
	private static final String TS = "1742505638683";
	// HMAC-SHA256 de "id:abc123;request-id:bb56a2f1-...;ts:1742505638683;" com a chave "segredo" (calculado fora do Java)
	private static final String V1 = "a4edc0956c2662d807cc2bf60dee316f65c49e71c47707ec296e347c521541e2";
	// HMAC-SHA256 de "ts:1742505638683;" (sem id e sem request-id)
	private static final String V1_SO_TS = "e49a4f2891c10f333c84fdcc914565b4f5cd26249e0b291ec25a4ca2a484e926";

	@Test
	void calculaOMesmoValorQueOMercadoPago() {
		assertEquals(V1, MercadoPagoAssinatura.calcular(SEGREDO, REQUISICAO, "ABC123", TS));
		assertEquals(V1_SO_TS, MercadoPagoAssinatura.calcular(SEGREDO, null, null, TS));
	}

	@Test
	void aceitaAssinaturaCorretaComIdEmMaiusculas() {
		assertTrue(MercadoPagoAssinatura.valida(SEGREDO, "ts=" + TS + ",v1=" + V1, REQUISICAO, "ABC123"));
		assertTrue(MercadoPagoAssinatura.valida(SEGREDO, "v1=" + V1.toUpperCase() + " , ts=" + TS, REQUISICAO, "abc123"));
	}

	@Test
	void recusaChaveErradaIdTrocadoOuCabecalhoRuim() {
		String cabecalho = "ts=" + TS + ",v1=" + V1;
		assertFalse(MercadoPagoAssinatura.valida("outro", cabecalho, REQUISICAO, "ABC123"));
		assertFalse(MercadoPagoAssinatura.valida(SEGREDO, cabecalho, REQUISICAO, "999"));
		assertFalse(MercadoPagoAssinatura.valida(SEGREDO, cabecalho, "outra-requisicao", "ABC123"));
		assertFalse(MercadoPagoAssinatura.valida(SEGREDO, "ts=1742505638684,v1=" + V1, REQUISICAO, "ABC123"));
		assertFalse(MercadoPagoAssinatura.valida(SEGREDO, "ts=" + TS, REQUISICAO, "ABC123"));
		assertFalse(MercadoPagoAssinatura.valida(SEGREDO, "lixo", REQUISICAO, "ABC123"));
		assertFalse(MercadoPagoAssinatura.valida(SEGREDO, null, REQUISICAO, "ABC123"));
		assertFalse(MercadoPagoAssinatura.valida("", cabecalho, REQUISICAO, "ABC123"));
	}
}
