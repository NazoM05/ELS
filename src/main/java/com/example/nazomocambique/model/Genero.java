package com.example.nazomocambique.model;

/**
 * Gênero / público do produto. UNISSEX serve para os dois públicos: na loja, quem filtra
 * por Masculino ou Feminino também enxerga os produtos Unissex.
 */
public enum Genero {
	MASCULINO("Masculino"),
	FEMININO("Feminino"),
	UNISSEX("Unissex");

	private final String rotulo;

	Genero(String rotulo) {
		this.rotulo = rotulo;
	}

	public String getRotulo() {
		return rotulo;
	}

	/** Converte o texto recebido na URL/formulário; vazio ou desconhecido vira null (= sem filtro). */
	public static Genero deTexto(String texto) {
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
