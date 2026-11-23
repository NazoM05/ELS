package com.example.nazomocambique.model;

import org.springframework.data.domain.Sort;

/**
 * Ordem da vitrine, escolhida pelo parâmetro ?ordem= (usado pelos links de Coleções do rodapé).
 * PADRAO mantém a ordem de cadastro; NOVIDADES mostra os últimos cadastrados primeiro;
 * MENOR_PRECO mostra os mais baratos primeiro (usado pelo Outlet).
 */
public enum Ordenacao {
	PADRAO("", "Produtos", Sort.by(Sort.Order.asc("id"))),
	NOVIDADES("novidades", "Novidades", Sort.by(Sort.Order.desc("id"))),
	MENOR_PRECO("menor-preco", "Outlet", Sort.by(Sort.Order.asc("preco"), Sort.Order.asc("id")));

	private final String parametro;
	private final String titulo;
	private final Sort sort;

	Ordenacao(String parametro, String titulo, Sort sort) {
		this.parametro = parametro;
		this.titulo = titulo;
		this.sort = sort;
	}

	/** Valor usado na URL (?ordem=...). Vazio para a ordem padrão. */
	public String getParametro() {
		return parametro;
	}

	/** Título da seção da vitrine quando esta ordem está ativa. */
	public String getTitulo() {
		return titulo;
	}

	public Sort getSort() {
		return sort;
	}

	/** Converte o texto da URL; vazio ou desconhecido vira PADRAO. */
	public static Ordenacao deTexto(String texto) {
		if (texto != null) {
			for (Ordenacao o : values()) {
				if (!o.parametro.isEmpty() && o.parametro.equalsIgnoreCase(texto.trim())) {
					return o;
				}
			}
		}
		return PADRAO;
	}
}
