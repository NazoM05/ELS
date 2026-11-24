package com.example.nazomocambique.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

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
		Duration timeout,
		Remetente remetente) {

	/** Construtor sem o remetente (mantém quem já criava as propriedades só com a cotação de frete). */
	public CorreiosProperties(boolean enabled, String baseUrl, String usuario, String codigoAcesso, String cartaoPostagem,
			String cepOrigem, List<String> servicos, Duration timeout) {
		this(enabled, baseUrl, usuario, codigoAcesso, cartaoPostagem, cepOrigem, servicos, timeout, null);
	}

	@ConstructorBinding
	public CorreiosProperties {
		baseUrl = vazio(baseUrl) ? "https://api.correios.com.br" : baseUrl.trim();
		usuario = usuario == null ? "" : usuario.trim();
		codigoAcesso = codigoAcesso == null ? "" : codigoAcesso.trim();
		cartaoPostagem = cartaoPostagem == null ? "" : cartaoPostagem.trim(); // texto: tem zeros à esquerda
		cepOrigem = cepOrigem == null ? "" : cepOrigem.replaceAll("\\D", "");
		servicos = servicos == null || servicos.isEmpty() ? List.of("03298", "03220") : List.copyOf(servicos);
		timeout = timeout == null ? Duration.ofSeconds(4) : timeout;
		remetente = remetente == null ? new Remetente(null, null, null, null, null, null, null, null, null, null) : remetente;
	}

	/**
	 * Dados de quem posta (a loja), impressos na etiqueta e enviados na pré-postagem. O CEP do remetente é o
	 * "cep-origem" acima. Prefixo "correios.remetente.".
	 */
	public record Remetente(String nome, String cpfCnpj, String telefone, String email, String logradouro, String numero,
			String complemento, String bairro, String cidade, String uf) {

		public Remetente {
			nome = limpo(nome);
			cpfCnpj = cpfCnpj == null ? "" : cpfCnpj.replaceAll("\\D", "");
			telefone = telefone == null ? "" : telefone.replaceAll("\\D", "");
			email = limpo(email);
			logradouro = limpo(logradouro);
			numero = limpo(numero);
			complemento = limpo(complemento);
			bairro = limpo(bairro);
			cidade = limpo(cidade);
			uf = limpo(uf).toUpperCase(java.util.Locale.ROOT);
		}

		/** Tem tudo o que os Correios exigem do remetente (complemento, telefone e e-mail são opcionais). */
		public boolean completo() {
			return !nome.isEmpty() && (cpfCnpj.length() == 11 || cpfCnpj.length() == 14) && !logradouro.isEmpty()
					&& !numero.isEmpty() && !bairro.isEmpty() && !cidade.isEmpty() && uf.length() == 2;
		}

		private static String limpo(String s) {
			return s == null ? "" : s.trim();
		}
	}

	/** Pode criar pré-postagem: integração ligada e com credenciais, e remetente completo. */
	public boolean postagemConfigurada() {
		return configurado() && remetente.completo();
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
