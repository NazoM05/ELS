package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Monta o código "Pix copia e cola" (BR Code estático, padrão EMV® QRCPS do Banco Central) com chave,
 * recebedor, valor e identificador. É o texto que vai dentro do QR Code e que o cliente cola no app do banco.
 *
 * Pix estático não avisa a loja quando é pago: a confirmação é feita pela equipe, conferindo o extrato
 * (o identificador do pedido aparece no comprovante). Para confirmação automática, a Nazo utiliza o Asaas.
 */
public final class PixBrCode {

	/** Pelo manual do BR Code, o campo 26 (conta do recebedor) cabe no máximo 99 caracteres. */
	private static final int CHAVE_MAX = 77;
	private static final int NOME_MAX = 25;
	private static final int CIDADE_MAX = 15;
	private static final int TXID_MAX = 25;
	private static final BigDecimal VALOR_MAX = new BigDecimal("9999999999.99");

	private PixBrCode() {
	}

	/**
	 * @param chave  chave Pix do recebedor (CPF/CNPJ só dígitos, e-mail, celular com +55 ou chave aleatória)
	 * @param nome   nome do recebedor (até 25 caracteres; acentos são removidos)
	 * @param cidade cidade do recebedor (até 15 caracteres; acentos são removidos)
	 * @param valor  valor a pagar, maior que zero
	 * @param txid   identificador da cobrança (só letras e números, até 25); vazio vira "***"
	 * @throws IllegalArgumentException se algum dado não pode ser colocado no código
	 */
	public static String gerar(String chave, String nome, String cidade, BigDecimal valor, String txid) {
		String chaveLimpa = chave == null ? "" : chave.trim();
		if (chaveLimpa.isEmpty() || chaveLimpa.length() > CHAVE_MAX || !ascii(chaveLimpa)) {
			throw new IllegalArgumentException("Chave Pix inválida.");
		}
		if (valor == null || valor.signum() <= 0 || valor.compareTo(VALOR_MAX) > 0) {
			throw new IllegalArgumentException("O valor do Pix deve ser maior que zero.");
		}
		String nomeLimpo = texto(nome, NOME_MAX);
		String cidadeLimpa = texto(cidade, CIDADE_MAX);
		if (nomeLimpo.isEmpty() || cidadeLimpa.isEmpty()) {
			throw new IllegalArgumentException("Nome e cidade do recebedor são obrigatórios.");
		}

		String conta = campo("00", "br.gov.bcb.pix") + campo("01", chaveLimpa);
		String adicional = campo("05", identificador(txid));

		String semCrc = campo("00", "01") // versão do formato
				+ campo("01", "11") // 11 = estático (pode ser pago mais de uma vez)
				+ campo("26", conta)
				+ campo("52", "0000") // categoria do comércio não informada
				+ campo("53", "986") // moeda: real
				+ campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
				+ campo("58", "BR")
				+ campo("59", nomeLimpo)
				+ campo("60", cidadeLimpa)
				+ campo("62", adicional)
				+ "6304";
		return semCrc + crc16(semCrc);
	}

	/** CRC16-CCITT (polinômio 0x1021, início 0xFFFF) em 4 dígitos hexadecimais maiúsculos. */
	static String crc16(String texto) {
		int crc = 0xFFFF;
		for (byte b : texto.getBytes(StandardCharsets.US_ASCII)) {
			crc ^= (b & 0xFF) << 8;
			for (int i = 0; i < 8; i++) {
				crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
				crc &= 0xFFFF;
			}
		}
		return String.format(Locale.ROOT, "%04X", crc);
	}

	/** Identificador (txid): só letras e números, maiúsculas. Ex.: "EL-261120-7K3Q" vira "EL2611207K3Q". */
	static String identificador(String txid) {
		String limpo = txid == null ? "" : txid.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
		if (limpo.length() > TXID_MAX) {
			limpo = limpo.substring(0, TXID_MAX);
		}
		return limpo.isEmpty() ? "***" : limpo;
	}

	private static String campo(String id, String valor) {
		return id + String.format(Locale.ROOT, "%02d", valor.length()) + valor;
	}

	/** Maiúsculas, sem acentos e só com caracteres ASCII imprimíveis, cortado no limite do campo. */
	private static String texto(String original, int max) {
		if (original == null) {
			return "";
		}
		String semAcento = Normalizer.normalize(original, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		String limpo = semAcento.replaceAll("[^\\x20-\\x7E]", "").trim().toUpperCase(Locale.ROOT);
		return limpo.length() > max ? limpo.substring(0, max).trim() : limpo;
	}

	private static boolean ascii(String s) {
		return s.chars().allMatch(c -> c >= 0x20 && c <= 0x7E);
	}
}
