package com.example.nazomocambique.model;

/** Prazo de entrega de um estado, em dias úteis contados a partir do envio. */
public record PrazoEntrega(String uf, String nome, String regiao, int diasMin, int diasMax) {

	public PrazoEntrega {
		if (diasMin < 1 || diasMax < diasMin) {
			throw new IllegalArgumentException("Prazo inválido para " + uf + ": " + diasMin + " a " + diasMax);
		}
	}

	/** Texto pronto para exibir, por exemplo "2 a 4 dias úteis". */
	public String getDescricao() {
		return diasMin + " a " + diasMax + " dias úteis";
	}
}
