package com.example.nazomocambique.model;

/**
 * Categoria do produto (tipo de peça). Gravada no banco pelo nome da constante (ex.: CALCA);
 * o rótulo é o texto mostrado nas telas e nos filtros da loja.
 * Para criar uma categoria nova, basta acrescentar uma linha aqui.
 */
public enum Categoria {
	ACESSORIOS("Acessórios"),
	BERMUDA("Bermuda e Short"),
	BLUSA("Blusa"),
	CALCA("Calça"),
	CAMISA("Camisa"),
	CAMISETA("Camiseta"),
	JAQUETA("Jaqueta"),
	MOLETOM("Moletom"),
	SAIA("Saia"),
	VESTIDO("Vestido");

	private final String rotulo;

	Categoria(String rotulo) {
		this.rotulo = rotulo;
	}

	public String getRotulo() {
		return rotulo;
	}

	/** Converte o texto recebido na URL/formulário; vazio ou desconhecido vira null (= sem filtro). */
	public static Categoria deTexto(String texto) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		try {
			return valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
