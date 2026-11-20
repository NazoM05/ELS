package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/** Gateway de pagamento (a implementação real é o MercadoPagoClient; nos testes entra um falso). */
public interface GatewayPagamento {

	/** Dados para cobrar um Pix. A chave de idempotência evita cobrança duplicada se a chamada for repetida. */
	record SolicitacaoPix(String referencia, BigDecimal valor, String descricao, String email, String nome, String cpf,
			int minutosValidade, String chaveIdempotencia) {
	}

	/** Pix criado: código "copia e cola", imagem do QR Code em base64 e até quando vale. */
	record Pix(long paymentId, String status, String copiaECola, String qrBase64, OffsetDateTime expiraEm) {
	}

	/** Pagamento como o gateway informa (sempre consultado direto na API, nunca confiado do webhook). */
	record Pagamento(long id, String status, String referencia, BigDecimal valor) {
	}

	/** @throws PagamentoIndisponivelException se o gateway recusou ou não respondeu */
	Pix criarPix(SolicitacaoPix solicitacao);

	/** Vazio se o gateway não conhece esse pagamento. @throws PagamentoIndisponivelException se não respondeu */
	Optional<Pagamento> consultar(long paymentId);
}
