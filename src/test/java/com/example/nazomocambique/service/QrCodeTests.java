package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

class QrCodeTests {
	@Test void qrGeradoPodeSerLidoERecuperaConteudo() throws Exception {
		String payload = "00020101021126370014BR.GOV.BCB.PIX0115dev@example.com5204000053039865406209.905802BR5915NAZO MOÇAMBIQUE6009PORTO ALEGRE62160512EL2611207K3Q6304ABCD";
		String uri = QrCode.codificar(payload).paraDataUri(4);
		assertTrue(uri.startsWith("data:image/png;base64,"));
		byte[] bytes = Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1));
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
		var source = new BufferedImageLuminanceSource(image);
		var bitmap = new BinaryBitmap(new HybridBinarizer(source));
		assertEquals(payload, new MultiFormatReader().decode(bitmap).getText());
	}
}
