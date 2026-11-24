package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.nazomocambique.config.PixProperties;
import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;

class PixEstaticoServiceTests {

	private static final PixProperties CONFIGURADO = new PixProperties("dev@example.com", "Nazo Moçambique", "Porto Alegre");

	private static Pedido pedido() {
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
	void geraOPixComOValorTotalEOCodigoDoPedido() {
		var pix = new PixEstaticoService(CONFIGURADO).gerar(pedido()).orElseThrow();
		assertEquals(new BigDecimal("209.90"), pix.valor());
		assertTrue(pix.copiaECola().contains("5406209.90"), pix.copiaECola());
		assertTrue(pix.copiaECola().contains("0512EL2611207K3Q"), pix.copiaECola());
		assertTrue(pix.qrDataUri().startsWith("data:image/png;base64,"));
	}

	@Test
	void cobraOTotalIncluindoFrete() {
		var pix = new PixEstaticoService(CONFIGURADO).gerar(pedido()).orElseThrow();
		assertTrue(pix.copiaECola().contains("209.90"), pix.copiaECola());
	}

	@Test
	void semRecebedorConfiguradoNaoGera() {
		assertTrue(new PixEstaticoService(new PixProperties("", "", "")).gerar(pedido()).isEmpty());
		assertTrue(new PixEstaticoService(new PixProperties("dev@example.com", "Loja", null)).gerar(pedido()).isEmpty());
		assertFalse(new PixEstaticoService(new PixProperties(null, null, null)).configurado());
	}

	@Test
	void soGeraParaPedidoPixAguardandoPagamento() {
		PixEstaticoService servico = new PixEstaticoService(CONFIGURADO);

		Pedido pago = pedido();
		pago.setStatus(StatusPedido.PAGO);
		assertTrue(servico.gerar(pago).isEmpty());

		Pedido cartao = pedido();
		cartao.setFormaPagamento(FormaPagamento.CARTAO);
		assertTrue(servico.gerar(cartao).isEmpty());

		assertTrue(servico.gerar(null).isEmpty());
	}

	@Test
	void totalZeradoNaoGeraPixEnaoQuebra() {
		Pedido p = pedido();
		p.setTotal(BigDecimal.ZERO);
		assertTrue(new PixEstaticoService(CONFIGURADO).gerar(p).isEmpty());
	}
}
