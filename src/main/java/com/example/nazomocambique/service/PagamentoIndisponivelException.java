package com.example.nazomocambique.service;

/** O pagamento não pôde ser gerado ou conferido (Asaas fora do ar, pedido inválido, etc.). A mensagem pode ser mostrada ao cliente. */
public class PagamentoIndisponivelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PagamentoIndisponivelException(String mensagem) {
		super(mensagem);
	}

	public PagamentoIndisponivelException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
