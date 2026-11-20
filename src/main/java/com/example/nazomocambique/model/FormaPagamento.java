package com.example.nazomocambique.model;

import java.util.Locale;

/** Formas de pagamento oferecidas na finalização da compra. Gravadas pelo nome da constante. */
public enum FormaPagamento {
	PIX("Pix"),
	CARTAO("Cartão de crédito"),
	BOLETO("Boleto bancário");

	private final String rotulo;

	FormaPagamento(String rotulo) {
		this.rotulo = rotulo;
	}

	public String getRotulo() {
		return rotulo;
	}

	/** Converte o valor do formulário; vazio ou desconhecido vira null. */
	public static FormaPagamento deTexto(String texto) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		try {
			return valueOf(texto.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
