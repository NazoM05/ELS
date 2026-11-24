package com.example.nazomocambique.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** QR Code Pix robusto, gerado pela biblioteca ZXing e exportado em PNG. */
public final class QrCode {
	private final BitMatrix matriz;
	private QrCode(BitMatrix matriz) { this.matriz = matriz; }
	public static QrCode codificar(String texto) {
		if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Conteúdo do QR Code vazio.");
		try {
			var hints = new java.util.HashMap<EncodeHintType, Object>();
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
			hints.put(EncodeHintType.MARGIN, 4);
			return new QrCode(new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 320, 320, hints));
		} catch (WriterException e) {
			throw new IllegalArgumentException("Não foi possível gerar o QR Code.", e);
		}
	}
	public int getTamanho() { return matriz.getWidth(); }
	public int getVersao() { return Math.max(1, (getTamanho() - 17) / 4); }
	public boolean modulo(int x, int y) { return x >= 0 && y >= 0 && x < matriz.getWidth() && y < matriz.getHeight() && matriz.get(x, y); }
	public String paraSvg(int borda) {
		int w = matriz.getWidth(), h = matriz.getHeight();
		int totalW = w + borda * 2, totalH = h + borda * 2;
		StringBuilder sb = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(totalW).append(' ').append(totalH).append("\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"white\"/><path fill=\"black\" d=\"");
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (matriz.get(x,y)) sb.append('M').append(x+borda).append(',').append(y+borda).append("h1v1h-1z");
		return sb.append("\"/></svg>").toString();
	}
	public String paraDataUri(int borda) {
		try {
			int w = matriz.getWidth(), h = matriz.getHeight();
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			for (int y=0;y<h;y++) for(int x=0;x<w;x++) image.setRGB(x,y,matriz.get(x,y) ? 0xFF000000 : 0xFFFFFFFF);
			ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(image, "png", out);
			return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
		} catch (IOException e) { throw new IllegalStateException("Falha ao exportar QR Code PNG.", e); }
	}
}
