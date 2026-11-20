package com.example.nazomocambique.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da integração com o Mercado Pago (pagamento por Pix). Tudo vem do application.properties
 * (prefixo "mercadopago.") ou de variáveis de ambiente; nenhuma chave dentro do código.
 *
 * Sem credenciais, deixe mercadopago.enabled=false: o checkout continua gravando o pedido como
 * "Aguardando pagamento" e a equipe confirma o pagamento à mão, como antes.
 */
@ConfigurationProperties(prefix = "mercadopago")
public record MercadoPagoProperties(
		boolean enabled,
		String baseUrl,
		String accessToken,
		String webhookSecret,
		String notificationUrl,
		Integer pixExpiracaoMinutos,
		Duration timeout) {

	/** O Pix do Mercado Pago aceita validade de 30 minutos a 30 dias. */
	private static final int PIX_MINIMO = 30;
	private static final int PIX_MAXIMO = 30 * 24 * 60;

	public MercadoPagoProperties {
		baseUrl = vazio(baseUrl) ? "https://api.mercadopago.com" : baseUrl.trim();
		accessToken = accessToken == null ? "" : accessToken.trim();
		webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
		notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
		int minutos = pixExpiracaoMinutos == null ? 60 : pixExpiracaoMinutos;
		pixExpiracaoMinutos = Math.min(Math.max(minutos, PIX_MINIMO), PIX_MAXIMO);
		timeout = timeout == null ? Duration.ofSeconds(8) : timeout;
	}

	/** Só cobra pelo Mercado Pago se estiver ligado e com o Access Token preenchido. */
	public boolean configurado() {
		return enabled && !accessToken.isEmpty();
	}

	/** O webhook só é aceito com a assinatura secreta configurada (sem ela, qualquer um poderia chamá-lo). */
	public boolean webhookConfigurado() {
		return !webhookSecret.isEmpty();
	}

	private static boolean vazio(String s) {
		return s == null || s.isBlank();
	}
}
