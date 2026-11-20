package com.example.nazomocambique.service;

import java.util.List;
import java.util.Map;

/**
 * A compra não pôde ser finalizada. "errosCampos" traz os problemas do formulário de entrega
 * (campo -> mensagem) e "mensagens" os problemas do carrinho (estoque, produto indisponível...).
 * Como a exceção sai do método @Transactional, nenhum estoque é alterado.
 */
public class CheckoutException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Map<String, String> errosCampos;
	private final List<String> mensagens;

	public CheckoutException(Map<String, String> errosCampos, List<String> mensagens) {
		super(mensagens.isEmpty() ? "Dados de entrega inválidos." : String.join(" ", mensagens));
		this.errosCampos = errosCampos;
		this.mensagens = mensagens;
	}

	public Map<String, String> getErrosCampos() {
		return errosCampos;
	}

	public List<String> getMensagens() {
		return mensagens;
	}
}
