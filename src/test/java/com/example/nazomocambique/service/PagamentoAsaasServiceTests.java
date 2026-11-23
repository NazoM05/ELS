package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.nazomocambique.config.AsaasProperties;
import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.DadosEntrega;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.ItemCarrinho;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.repository.ProdutoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;

/** Pix do Asaas: gerar a cobrança e confirmar o pedido pelo aviso do webhook (com um gateway falso, sem internet). */
@SpringBootTest
class PagamentoAsaasServiceTests {

	private static final String ID_ASAAS = "pay_555";

	@Autowired
	private PedidoService pedidoService;
	@Autowired
	private GestaoPedidoService gestao;
	@Autowired
	private PedidoRepository pedidoRepository;
	@Autowired
	private ProdutoRepository produtoRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private GatewayFalso gateway;
	private PagamentoAsaasService servico;
	private Long produtoId;
	private Long usuarioId;

	/** Gateway sem internet: conta quantos Pix foram criados e devolve o pagamento que o teste mandar. */
	private static class GatewayFalso implements GatewayPagamento {
		int pixCriados;
		int consultas;
		boolean falhar;
		Pagamento pagamento;

		@Override
		public Pix criarPix(SolicitacaoPix s) {
			pixCriados++;
			return new Pix(ID_ASAAS, "PENDING", "00020126COPIAECOLA", "iVBORw0KGgo=", OffsetDateTime.now().plusMinutes(60));
		}

		@Override
		public Optional<Pagamento> consultar(String paymentId) {
			consultas++;
			if (falhar) {
				throw new PagamentoIndisponivelException("Asaas fora do ar (teste).");
			}
			return Optional.ofNullable(pagamento);
		}
	}

	@BeforeEach
	void preparar() {
		gateway = new GatewayFalso();
		AsaasProperties props = new AsaasProperties(true, null, "TOKEN", "01234567890123456789012345678901", 60, null);
		servico = new PagamentoAsaasService(props, gateway, pedidoRepository, transactionManager);

		Produto p = new Produto();
		p.setNome("Produto pix " + System.nanoTime());
		p.setPreco(50.0f);
		p.setTamanho("P,M");
		p.setCategoria(Categoria.CAMISETA);
		p.setGenero(Genero.UNISSEX);
		Map<String, Integer> estoque = new LinkedHashMap<>();
		estoque.put("P", 5);
		estoque.put("M", 5);
		p.setEstoque(estoque);
		produtoId = produtoRepository.save(p).getId();

		usuarioId = novoUsuario();
	}

	private Long novoUsuario() {
		Usuario u = new Usuario();
		u.setUsername("pix" + System.nanoTime() + "@teste.com");
		u.setNome("Comprador Pix");
		u.setRole("ROLE_USUARIO");
		return usuarioRepository.save(u).getId();
	}

	private Pedido vender() {
		DadosEntrega d = new DadosEntrega();
		d.setNome("Comprador Pix");
		d.setTelefone("(51) 99999-9999");
		d.setCep("90000-000");
		d.setEndereco("Rua A");
		d.setNumero("1");
		d.setBairro("Centro");
		d.setCidade("Porto Alegre");
		d.setUf("RS");
		d.setFormaPagamento("PIX");
		return pedidoService.finalizar(usuarioId, d, List.of(new ItemCarrinho(produtoId, "M", 1)));
	}

	private Pedido ler(Pedido p) {
		return gestao.buscarPorCodigo(p.getCodigo()).orElseThrow();
	}

	@Test
	void pixGeradoFicaGravadoNoPedidoSemMudarAEtapa() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);

		Pedido lido = ler(p);
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, lido.getStatus());
		assertEquals(ID_ASAAS, lido.getAsaasPaymentId());
		assertEquals("00020126COPIAECOLA", lido.getPixCopiaECola());
		assertTrue(lido.isPixAtivo());
	}

	@Test
	void pixAindaValidoNaoEhGeradoDeNovo() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);
		servico.gerarPix(p.getId(), usuarioId);

		assertEquals(1, gateway.pixCriados);
	}

	@Test
	void pedidoDeOutroClienteNaoGeraPix() {
		Pedido p = vender();
		Long outro = novoUsuario();

		assertThrows(PagamentoIndisponivelException.class, () -> servico.gerarPix(p.getId(), outro));
		assertEquals(0, gateway.pixCriados);
	}

	@Test
	void pagamentoAprovadoConfirmaOPedidoUmaVezSo() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);
		gateway.pagamento = new GatewayPagamento.Pagamento(ID_ASAAS, "RECEIVED", p.getCodigo(), p.getTotal());

		servico.processarNotificacao(ID_ASAAS);
		Pedido pago = ler(p);
		assertEquals(StatusPedido.PAGO, pago.getStatus());
		assertEquals("RECEIVED", pago.getAsaasStatus());
		int linhas = pago.getHistorico().size();

		servico.processarNotificacao(ID_ASAAS); // aviso repetido
		assertEquals(linhas, ler(p).getHistorico().size());
		assertEquals("Asaas", ler(p).getHistoricoRecente().get(0).getResponsavel());
	}

	@Test
	void pagamentoPendenteOuComValorDiferenteNaoConfirma() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);

		gateway.pagamento = new GatewayPagamento.Pagamento(ID_ASAAS, "PENDING", p.getCodigo(), p.getTotal());
		servico.processarNotificacao(ID_ASAAS);
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, ler(p).getStatus());

		gateway.pagamento = new GatewayPagamento.Pagamento(ID_ASAAS, "RECEIVED", p.getCodigo(), p.getTotal().subtract(BigDecimal.ONE));
		servico.processarNotificacao(ID_ASAAS);
		Pedido lido = ler(p);
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, lido.getStatus());
		assertTrue(lido.getHistoricoRecente().get(0).getObservacao().contains("valor diferente"));
	}

	@Test
	void pagamentoAprovadoDepoisDoCancelamentoNaoReabreOPedido() {
		Pedido p = vender();
		gestao.atualizar(p.getCodigo(), StatusPedido.CANCELADO, "", "Cliente desistiu", "Ana");
		gateway.pagamento = new GatewayPagamento.Pagamento(ID_ASAAS, "RECEIVED", p.getCodigo(), p.getTotal());

		servico.processarNotificacao(ID_ASAAS);

		Pedido lido = ler(p);
		assertEquals(StatusPedido.CANCELADO, lido.getStatus());
		assertNotNull(lido.getHistoricoRecente().get(0).getObservacao());
		assertTrue(lido.getHistoricoRecente().get(0).getObservacao().contains("Devolver"));
	}

	@Test
	void pagamentoDesconhecidoOuSemPedidoEhIgnorado() {
		gateway.pagamento = null;
		servico.processarNotificacao("pay_999");

		gateway.pagamento = new GatewayPagamento.Pagamento("pay_999", "RECEIVED", "EL-000000-ZZZZ", new BigDecimal("10.00"));
		servico.processarNotificacao("pay_999");
	}

	@Test
	void sincronizarPedidoConfirmaPagamentoQueOWebhookNaoAvisou() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);
		gateway.pagamento = new GatewayPagamento.Pagamento(ID_ASAAS, "RECEIVED", p.getCodigo(), p.getTotal());

		assertTrue(servico.sincronizarPedido(p.getId(), usuarioId));

		assertEquals(StatusPedido.PAGO, ler(p).getStatus());
	}

	@Test
	void sincronizarPedidoNaoConsultaOAsaasMaisQueUmaVezPorIntervalo() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);
		gateway.pagamento = new GatewayPagamento.Pagamento(ID_ASAAS, "PENDING", p.getCodigo(), p.getTotal());

		assertTrue(servico.sincronizarPedido(p.getId(), usuarioId));
		assertTrue(!servico.sincronizarPedido(p.getId(), usuarioId));

		assertEquals(1, gateway.consultas);
	}

	@Test
	void sincronizarPedidoIgnoraPedidoSemCobrancaOuDeOutroCliente() {
		Pedido semPix = vender();
		Pedido comPix = vender();
		servico.gerarPix(comPix.getId(), usuarioId);
		Long outro = novoUsuario();

		assertTrue(!servico.sincronizarPedido(semPix.getId(), usuarioId));
		assertTrue(!servico.sincronizarPedido(comPix.getId(), outro));
		assertEquals(0, gateway.consultas);
	}

	@Test
	void falhaDoAsaasNaSincronizacaoNaoQuebraENaoMudaOPedido() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);
		gateway.falhar = true;

		assertTrue(!servico.sincronizarPedido(p.getId(), usuarioId));

		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, ler(p).getStatus());
	}

	@Test
	void sincronizarPendentesConfirmaOsPedidosPagos() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);
		gateway.pagamento = new GatewayPagamento.Pagamento(ID_ASAAS, "RECEIVED", p.getCodigo(), p.getTotal());

		servico.sincronizarPendentes();

		assertEquals(StatusPedido.PAGO, ler(p).getStatus());
	}
}
