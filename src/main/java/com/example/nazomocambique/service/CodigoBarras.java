package com.example.nazomocambique.service;

import java.util.Map;
import java.util.regex.Pattern;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

/**
 * Desenha código de barras Code 128 como SVG (vetorial: sai nítido em impressora térmica e em A4).
 * Só codifica o que recebe; quem chama passa dados reais (código do objeto dos Correios, CEP do pedido).
 */
public final class CodigoBarras {

	/** Código de objeto dos Correios: 2 letras, 9 dígitos, 2 letras (ex.: AA123456789BR). */
	private static final Pattern CODIGO_OBJETO = Pattern.compile("[A-Z]{2}\\d{9}[A-Z]{2}");

	private CodigoBarras() {
	}

	public static boolean codigoObjetoValido(String codigo) {
		return codigo != null && CODIGO_OBJETO.matcher(codigo).matches();
	}

	/** SVG do Code 128 do código do objeto; vazio se o código não tiver o formato dos Correios. */
	public static String svgCodigoObjeto(String codigo) {
		return codigoObjetoValido(codigo) ? svg128(codigo) : "";
	}

	/** SVG do Code 128 do CEP (8 dígitos); vazio se o CEP for inválido. */
	public static String svgCep(String cep) {
		return cep != null && cep.matches("\\d{8}") ? svg128(cep) : "";
	}

	static String svg128(String conteudo) {
		// Largura 1: sem esticar; margem 0: a zona de silêncio fica por conta do layout da etiqueta
		BitMatrix m = new Code128Writer().encode(conteudo, BarcodeFormat.CODE_128, 1, 1,
				Map.of(EncodeHintType.MARGIN, 0));
		int largura = m.getWidth();
		StringBuilder d = new StringBuilder();
		int x = 0;
		while (x < largura) {
			if (!m.get(x, 0)) {
				x++;
				continue;
			}
			int inicio = x;
			while (x < largura && m.get(x, 0)) {
				x++;
			}
			d.append('M').append(inicio).append(" 0h").append(x - inicio).append("v1h-").append(x - inicio).append('z');
		}
		return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + largura
				+ " 1\" preserveAspectRatio=\"none\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"Código de barras "
				+ conteudo + "\"><path d=\"" + d + "\"/></svg>";
	}
}
