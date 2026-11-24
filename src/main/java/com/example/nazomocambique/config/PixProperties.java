package com.example.nazomocambique.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dados do recebedor do Pix estático (QR Code com o valor total do pedido). Tudo vem do
 * application.properties (prefixo "pix.") ou de variáveis de ambiente; nenhuma chave real dentro do código.
 *
 * Sem chave, nome e cidade o QR Code simplesmente não aparece na tela do pedido.
 */
@ConfigurationProperties(prefix = "pix")
public record PixProperties(String chave, String nome, String cidade) {

	public PixProperties {
		chave = limpar(chave);
		nome = limpar(nome);
		cidade = limpar(cidade);
	}

	/** Só gera o Pix se os três dados do recebedor estiverem preenchidos. */
	public boolean configurado() {
		return !chave.isEmpty() && !nome.isEmpty() && !cidade.isEmpty();
	}

	private static String limpar(String s) {
		return s == null ? "" : s.trim();
	}
}
