package com.example.nazomocambique.model;

import java.util.Locale;

/**
 * Etapas de um pedido, na ordem em que a equipe costuma avançar.
 * Gravado pelo nome da constante. CANCELADO é final: o estoque já voltou para a loja.
 */
public enum StatusPedido {
	AGUARDANDO_PAGAMENTO("Aguardando pagamento", "aguardando"),
	PAGO("Pagamento confirmado", "pago"),
	EM_SEPARACAO("Em separação", "separacao"),
	SEPARADO("Separado", "separado"),
	ENVIADO("Enviado", "enviado"),
	ENTREGUE("Entregue", "entregue"),
	CANCELADO("Cancelado", "cancelado");

	private final String rotulo;
	private final String classe;

	StatusPedido(String rotulo, String classe) {
		this.rotulo = rotulo;
		this.classe = classe;
	}

	public String getRotulo() {
		return rotulo;
	}

	/** Sufixo da classe CSS da etiqueta (ex.: "status-pedido-enviado"). */
	public String getClasse() {
		return classe;
	}

	/** Pedido ainda precisa de ação da equipe. */
	public boolean isEmAberto() {
		return this == AGUARDANDO_PAGAMENTO || this == PAGO || this == EM_SEPARACAO || this == SEPARADO;
	}

	/** Vazio ou desconhecido vira null. */
	public static StatusPedido deTexto(String texto) {
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
