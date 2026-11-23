package com.example.nazomocambique.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Código de acompanhamento do pedido, ex.: EL-261120-7K3Q
 * "EL" + data da compra (aammdd) + 4 caracteres aleatórios. Sem 0/O, 1/I/L para não confundir
 * quando alguém dita o código por telefone ou WhatsApp.
 */
public final class CodigoPedido {

	public static final String PREFIXO = "EL";
	static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	static final int TAMANHO_SUFIXO = 4;
	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyMMdd");
	private static final Pattern FORMATO = Pattern.compile("^" + PREFIXO + "-\\d{6}-[" + ALFABETO + "]{" + TAMANHO_SUFIXO + "}$");
	private static final Random ALEATORIO = new SecureRandom();

	private CodigoPedido() {
	}

	public static String gerar(LocalDate data) {
		return gerar(data, ALEATORIO);
	}

	static String gerar(LocalDate data, Random aleatorio) {
		StringBuilder sb = new StringBuilder(PREFIXO).append('-').append(data.format(DATA)).append('-');
		for (int i = 0; i < TAMANHO_SUFIXO; i++) {
			sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
		}
		return sb.toString();
	}

	/** Aceita o código digitado com espaços, minúsculas ou sem os hífens (ex.: "el2611207k3q"). */
	public static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String limpo = texto.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
		if (limpo.length() == PREFIXO.length() + 6 + TAMANHO_SUFIXO && limpo.startsWith(PREFIXO)) {
			return PREFIXO + "-" + limpo.substring(2, 8) + "-" + limpo.substring(8);
		}
		return texto.trim().toUpperCase(Locale.ROOT);
	}

	public static boolean valido(String codigo) {
		return codigo != null && FORMATO.matcher(codigo).matches();
	}
}
