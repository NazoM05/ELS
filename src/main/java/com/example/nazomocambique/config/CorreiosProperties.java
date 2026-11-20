package com.example.nazomocambique.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da integração com a API de frete dos Correios (CWS). Tudo vem do application.properties
 * (prefixo "correios.") ou de variáveis de ambiente; nada de senha dentro do código.
 *
 * Sem contrato, deixe correios.enabled=false: a loja continua com o frete fixo e o prazo por estado.
 */
@ConfigurationProperties(prefix = "correios")
public record CorreiosProperties(
		boolean enabled,
		String baseUrl,
		String usuario,
		String codigoAcesso,
		String cartaoPostagem,
		String cepOrigem,
		List<String> servicos,
		Duration timeout) {

	public CorreiosProperties {
		baseUrl = vazio(baseUrl) ? "https://api.correios.com.br" : baseUrl.trim();
		usuario = usuario == null ? "" : usuario.trim();
		codigoAcesso = codigoAcesso == null ? "" : codigoAcesso.trim();
		cartaoPostagem = cartaoPostagem == null ? "" : cartaoPostagem.trim(); // texto: tem zeros à esquerda
		cepOrigem = cepOrigem == null ? "" : cepOrigem.replaceAll("\\D", "");
		servicos = servicos == null || servicos.isEmpty() ? List.of("03298", "03220") : List.copyOf(servicos);
		timeout = timeout == null ? Duration.ofSeconds(4) : timeout;
	}

	/** Só consulta os Correios se estiver ligado e com todas as credenciais preenchidas. */
	public boolean configurado() {
		return enabled && !usuario.isEmpty() && !codigoAcesso.isEmpty() && !cartaoPostagem.isEmpty()
				&& cepOrigem.length() == 8;
	}

	private static boolean vazio(String s) {
		return s == null || s.isBlank();
	}
}
