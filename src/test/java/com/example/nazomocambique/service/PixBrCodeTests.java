package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

class PixBrCodeTests {

	private static String gerar(BigDecimal valor, String txid) {
		return PixBrCode.gerar("dev@example.com", "Nazo Moçambique", "Porto Alegre", valor, txid);
	}

	private static Pedido pedidoPix() {
		Pedido p = new Pedido();
		p.setCodigo("EL-261120-7K3Q");
		p.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
		p.setFormaPagamento(FormaPagamento.PIX);
		p.setSubtotal(new BigDecimal("189.90"));
		p.setFrete(new BigDecimal("20.00"));
		p.setTotal(new BigDecimal("209.90"));
		return p;
	}

	@Test
	void crcConfereComExemploDoManualDoBancoCentral() {
		String exemplo = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000"
				+ "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304";
		assertEquals("1D3D", PixBrCode.crc16(exemplo));
	}


	@Test
	void crcSegueOPadraoCcittFalse() {
		// Vetor de teste padrão do CRC16/CCITT-FALSE
		assertEquals("29B1", PixBrCode.crc16("123456789"));
	}

	@Test
	void codigoTemOsCamposObrigatoriosDoBrCode() {
		String c = gerar(new BigDecimal("189.90"), "EL-261120-7K3Q");
		assertTrue(c.startsWith("000201010211"), c);
		assertTrue(c.contains("0014br.gov.bcb.pix0115dev@example.com"), c);
		assertTrue(c.contains("52040000"), c);
		assertTrue(c.contains("5303986"), c);
		assertTrue(c.contains("5802BR"), c);
		assertTrue(c.contains("5915NAZO MOCAMBIQUE"), c);
		assertTrue(c.contains("6012PORTO ALEGRE"), c);
	}

	@Test
	void valorSempreComDuasCasasEComPonto() {
		assertTrue(gerar(new BigDecimal("189.9"), "A").contains("5406189.90"));
		assertTrue(gerar(new BigDecimal("10"), "A").contains("540510.00"));
		assertTrue(gerar(new BigDecimal("0.01"), "A").contains("54040.01"));
		assertTrue(gerar(new BigDecimal("1234.5"), "A").contains("54071234.50"));
	}

	@Test
	void imagemQrCodeDecodificaExatamenteOCopiaECola() throws Exception {
		var pix = new PixEstaticoService(new com.example.nazomocambique.config.PixProperties(
			"12345678000190", "Nazo Mocambique", "Porto Alegre"))
			.gerar(pedidoPix()).orElseThrow();
		String base64 = pix.qrDataUri().substring("data:image/png;base64,".length());
		var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
		var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
		assertEquals(pix.copiaECola(), new MultiFormatReader().decode(bitmap).getText());
	}


	@Test
	void chaveCnpjFormatadaViraSomenteDigitosMasEmailPermanece() {
		assertEquals("12345678000190", PixConfiguracaoService.normalizarChave("12.345.678/0001-90"));
		assertEquals("loja@example.com", PixConfiguracaoService.normalizarChave(" loja@example.com "));
	}

	@Test
	void crcFinalConfereComOCodigoTodo() {
		String c = gerar(new BigDecimal("189.90"), "EL-261120-7K3Q");
		String semCrc = c.substring(0, c.length() - 4);
		assertTrue(semCrc.endsWith("6304"), c);
		assertEquals(PixBrCode.crc16(semCrc), c.substring(c.length() - 4));
	}

	@Test
	void identificadorFicaSoComLetrasENumerosEVazioViraAsteriscos() {
		assertEquals("EL2611207K3Q", PixBrCode.identificador("EL-261120-7K3Q"));
		assertEquals("***", PixBrCode.identificador(null));
		assertEquals("***", PixBrCode.identificador(" - "));
		assertEquals(25, PixBrCode.identificador("A".repeat(40)).length());
		assertTrue(gerar(new BigDecimal("5"), "EL-261120-7K3Q").contains("62160512EL2611207K3Q"));
		assertTrue(gerar(new BigDecimal("5"), null).contains("62070503***"));
	}

	@Test
	void nomeECidadeSemAcentoEnomeLongoCortado() {
		String c = PixBrCode.gerar("dev@example.com", "José da Conceição de Albuquerque Júnior", "São Leopoldo do Sul Grande",
				new BigDecimal("5"), "A");
		assertFalse(c.matches(".*[^\\x20-\\x7E].*"), c);
		assertTrue(c.contains("5925JOSE DA CONCEICAO DE ALBU"), c);
		assertTrue(c.contains("6015SAO LEOPOLDO DO"), c);
	}

	@Test
	void recusaValorInvalidoEDadosFaltando() {
		assertThrows(IllegalArgumentException.class, () -> gerar(BigDecimal.ZERO, "A"));
		assertThrows(IllegalArgumentException.class, () -> gerar(new BigDecimal("-1"), "A"));
		assertThrows(IllegalArgumentException.class, () -> gerar(null, "A"));
		assertThrows(IllegalArgumentException.class,
				() -> PixBrCode.gerar(" ", "Loja", "Cidade", BigDecimal.ONE, "A"));
		assertThrows(IllegalArgumentException.class,
				() -> PixBrCode.gerar("dev@example.com", "", "Cidade", BigDecimal.ONE, "A"));
		assertThrows(IllegalArgumentException.class,
				() -> PixBrCode.gerar("dev@example.com", "Loja", " ", BigDecimal.ONE, "A"));
		assertThrows(IllegalArgumentException.class,
				() -> PixBrCode.gerar("x".repeat(78), "Loja", "Cidade", BigDecimal.ONE, "A"));
	}
}
