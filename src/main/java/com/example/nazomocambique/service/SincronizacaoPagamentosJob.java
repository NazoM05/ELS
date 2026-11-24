package com.example.nazomocambique.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rede de segurança do webhook do Asaas: a cada 2 minutos confere no Asaas os pedidos Pix ainda pendentes,
 * para que um aviso perdido (túnel fora do ar, falha de rede) não deixe um pedido pago como "Aguardando pagamento".
 * O intervalo pode ser trocado com a propriedade asaas.sincronizacao-intervalo-ms.
 */
@Component
public class SincronizacaoPagamentosJob {

	private final PagamentoAsaasService pagamentoService;

	public SincronizacaoPagamentosJob(PagamentoAsaasService pagamentoService) {
		this.pagamentoService = pagamentoService;
	}

	@Scheduled(initialDelay = 60_000, fixedDelayString = "${asaas.sincronizacao-intervalo-ms:120000}")
	public void sincronizar() {
		pagamentoService.sincronizarPendentes();
	}
}
