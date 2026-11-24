package com.example.nazomocambique.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asaas")
public record AsaasProperties(boolean enabled, String baseUrl, String apiKey, String webhookToken, Integer pixExpiracaoMinutos, Duration timeout) {
    public AsaasProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api-sandbox.asaas.com/v3" : baseUrl.trim();
        apiKey = apiKey == null ? "" : apiKey.trim();
        webhookToken = webhookToken == null ? "" : webhookToken.trim();
        pixExpiracaoMinutos = pixExpiracaoMinutos == null ? 60 : Math.max(5, Math.min(pixExpiracaoMinutos, 1440));
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
    }
    public boolean configurado() { return enabled && !apiKey.isBlank(); }
    public boolean webhookConfigurado() { return webhookToken.length() >= 32; }
}
