package com.example.nazomocambique.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import com.example.nazomocambique.config.AsaasProperties;
import com.example.nazomocambique.service.PagamentoAsaasService;

@RestController
public class AsaasWebhookController {
    private static final Logger log = LoggerFactory.getLogger(AsaasWebhookController.class);
    private final AsaasProperties props;
    private final PagamentoAsaasService service;
    public AsaasWebhookController(AsaasProperties props, PagamentoAsaasService service) { this.props = props; this.service = service; }

    @PostMapping("/webhooks/asaas")
    public ResponseEntity<Void> receber(@RequestHeader(name="asaas-access-token", required=false) String token, @RequestBody(required=false) Map<String,Object> body) {
        if (!props.webhookConfigurado() || token == null || !java.security.MessageDigest.isEqual(props.webhookToken().getBytes(java.nio.charset.StandardCharsets.UTF_8), token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            log.warn("Webhook Asaas recusado: token ausente ou inválido.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body == null) return ResponseEntity.badRequest().build();
        String event = String.valueOf(body.getOrDefault("event", ""));
        if (!event.equals("PAYMENT_CONFIRMED") && !event.equals("PAYMENT_RECEIVED") && !event.equals("PAYMENT_REFUNDED") && !event.equals("PAYMENT_OVERDUE") && !event.equals("PAYMENT_DELETED")) return ResponseEntity.ok().build();
        Object paymentObj = body.get("payment");
        if (!(paymentObj instanceof Map<?,?> payment)) return ResponseEntity.ok().build();
        Object rawId = payment.get("id");
        String id = rawId == null ? "" : rawId.toString();
        if (id.isBlank() || id.equals("null")) return ResponseEntity.badRequest().build();
        try { service.processarNotificacao(id); return ResponseEntity.ok().build(); }
        catch (RuntimeException e) { log.error("Falha ao processar webhook Asaas para cobrança {}", id, e); return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build(); }
    }
}
