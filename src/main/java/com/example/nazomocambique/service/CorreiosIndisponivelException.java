package com.example.nazomocambique.service;

/** A API dos Correios falhou (rede, credencial, serviço não contratado...). O frete cai para a regra fixa da loja. */
public class CorreiosIndisponivelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CorreiosIndisponivelException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}

	public CorreiosIndisponivelException(String mensagem) {
		super(mensagem);
	}
}
