package com.example.nazomocambique.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Confere a assinatura (cabeçalho x-signature) das notificações do Mercado Pago.
 *
 * O cabeçalho vem como "ts=1742505638683,v1=<hmac>". O texto assinado é
 * "id:{data.id em minúsculas};request-id:{x-request-id};ts:{ts};" (partes ausentes são omitidas),
 * com HMAC-SHA256 usando a "assinatura secreta" do painel de webhooks.
 */
public final class MercadoPagoAssinatura {

	private MercadoPagoAssinatura() {
	}

	public static boolean valida(String segredo, String xSignature, String xRequestId, String dataId) {
		if (segredo == null || segredo.isEmpty() || xSignature == null || xSignature.isBlank()) {
			return false;
		}
		String ts = null;
		String v1 = null;
		for (String parte : xSignature.split(",")) {
			int igual = parte.indexOf('=');
			if (igual <= 0) {
				continue;
			}
			String chave = parte.substring(0, igual).trim();
			String valor = parte.substring(igual + 1).trim();
			if (chave.equals("ts")) {
				ts = valor;
			} else if (chave.equals("v1")) {
				v1 = valor;
			}
		}
		if (ts == null || ts.isEmpty() || v1 == null || v1.isEmpty()) {
			return false;
		}
		String esperado = calcular(segredo, xRequestId, dataId, ts);
		return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
				v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
	}

	/** HMAC-SHA256 em hexadecimal minúsculo do texto assinado. Público para os testes. */
	public static String calcular(String segredo, String xRequestId, String dataId, String ts) {
		StringBuilder texto = new StringBuilder();
		if (dataId != null && !dataId.isEmpty()) {
			texto.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
		}
		if (xRequestId != null && !xRequestId.isEmpty()) {
			texto.append("request-id:").append(xRequestId).append(';');
		}
		texto.append("ts:").append(ts).append(';');
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(texto.toString().getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("Não foi possível calcular a assinatura.", e);
		}
	}
}
