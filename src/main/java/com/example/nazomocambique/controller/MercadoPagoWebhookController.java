package com.example.nazomocambique.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.nazomocambique.config.MercadoPagoProperties;
import com.example.nazomocambique.service.MercadoPagoAssinatura;
import com.example.nazomocambique.service.PagamentoIndisponivelException;
import com.example.nazomocambique.service.PagamentoMercadoPagoService;

/**
 * Recebe os avisos do Mercado Pago (cadastre a URL /webhooks/mercadopago no painel, evento "Pagamentos").
 * Rota pública, mas só atende quem assina o aviso com a chave secreta; e mesmo assim o status do pagamento
 * é sempre conferido na API antes de mexer no pedido.
 * Respostas: 200 = recebido (o Mercado Pago não reenvia), 401 = assinatura inválida, 5xx = tentar de novo.
 */
@RestController
public class MercadoPagoWebhookController {

	private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

	private final MercadoPagoProperties props;
	private final PagamentoMercadoPagoService pagamentoService;

	public MercadoPagoWebhookController(MercadoPagoProperties props, PagamentoMercadoPagoService pagamentoService) {
		this.props = props;
		this.pagamentoService = pagamentoService;
	}

	@PostMapping("/webhooks/mercadopago")
	public ResponseEntity<Void> receber(
			@RequestParam(name = "data.id", required = false) String dataId,
			@RequestParam(name = "type", required = false) String tipo,
			@RequestHeader(name = "x-signature", required = false) String assinatura,
			@RequestHeader(name = "x-request-id", required = false) String requisicao) {

		if (!props.webhookConfigurado()) {
			log.warn("Webhook do Mercado Pago recebido, mas mercadopago.webhook-secret não está configurado. Recusado.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (!MercadoPagoAssinatura.valida(props.webhookSecret(), assinatura, requisicao, dataId)) {
			log.warn("Webhook do Mercado Pago com assinatura inválida. Recusado.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (tipo != null && !tipo.equals("payment")) {
			return ResponseEntity.ok().build(); // outros eventos (ex.: merchant_order) não interessam
		}
		long paymentId;
		try {
			paymentId = Long.parseLong(dataId == null ? "" : dataId.trim());
		} catch (NumberFormatException e) {
			return ResponseEntity.badRequest().build();
		}
		try {
			pagamentoService.processarNotificacao(paymentId);
			return ResponseEntity.ok().build();
		} catch (PagamentoIndisponivelException e) {
			log.warn("Não deu para conferir o pagamento {}: {}", paymentId, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
		} catch (RuntimeException e) {
			log.error("Erro ao processar o pagamento {} do Mercado Pago", paymentId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
