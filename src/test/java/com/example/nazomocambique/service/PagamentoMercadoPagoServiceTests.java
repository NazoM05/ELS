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

import com.example.nazomocambique.config.MercadoPagoProperties;
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

/** Pix do Mercado Pago: gerar a cobrança e confirmar o pedido pelo aviso do webhook (com um gateway falso, sem internet). */
@SpringBootTest
class PagamentoMercadoPagoServiceTests {

	private static final long ID_MP = 555L;

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
	private PagamentoMercadoPagoService servico;
	private Long produtoId;
	private Long usuarioId;

	/** Gateway sem internet: conta quantos Pix foram criados e devolve o pagamento que o teste mandar. */
	private static class GatewayFalso implements GatewayPagamento {
		int pixCriados;
		Pagamento pagamento;

		@Override
		public Pix criarPix(SolicitacaoPix s) {
			pixCriados++;
			return new Pix(ID_MP, "pending", "00020126COPIAECOLA", "iVBORw0KGgo=", OffsetDateTime.now().plusMinutes(60));
		}

		@Override
		public Optional<Pagamento> consultar(long paymentId) {
			return Optional.ofNullable(pagamento);
		}
	}

	@BeforeEach
	void preparar() {
		gateway = new GatewayFalso();
		MercadoPagoProperties props = new MercadoPagoProperties(true, null, "TOKEN", "segredo", null, 60, null);
		servico = new PagamentoMercadoPagoService(props, gateway, pedidoRepository, transactionManager);

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
		assertEquals(ID_MP, lido.getMpPaymentId());
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
		gateway.pagamento = new GatewayPagamento.Pagamento(ID_MP, "approved", p.getCodigo(), p.getTotal());

		servico.processarNotificacao(ID_MP);
		Pedido pago = ler(p);
		assertEquals(StatusPedido.PAGO, pago.getStatus());
		assertEquals("approved", pago.getMpStatus());
		int linhas = pago.getHistorico().size();

		servico.processarNotificacao(ID_MP); // aviso repetido
		assertEquals(linhas, ler(p).getHistorico().size());
		assertEquals("Mercado Pago", ler(p).getHistoricoRecente().get(0).getResponsavel());
	}

	@Test
	void pagamentoPendenteOuComValorDiferenteNaoConfirma() {
		Pedido p = vender();
		servico.gerarPix(p.getId(), usuarioId);

		gateway.pagamento = new GatewayPagamento.Pagamento(ID_MP, "pending", p.getCodigo(), p.getTotal());
		servico.processarNotificacao(ID_MP);
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, ler(p).getStatus());

		gateway.pagamento = new GatewayPagamento.Pagamento(ID_MP, "approved", p.getCodigo(), p.getTotal().subtract(BigDecimal.ONE));
		servico.processarNotificacao(ID_MP);
		Pedido lido = ler(p);
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, lido.getStatus());
		assertTrue(lido.getHistoricoRecente().get(0).getObservacao().contains("valor diferente"));
	}

	@Test
	void pagamentoAprovadoDepoisDoCancelamentoNaoReabreOPedido() {
		Pedido p = vender();
		gestao.atualizar(p.getCodigo(), StatusPedido.CANCELADO, "", "Cliente desistiu", "Ana");
		gateway.pagamento = new GatewayPagamento.Pagamento(ID_MP, "approved", p.getCodigo(), p.getTotal());

		servico.processarNotificacao(ID_MP);

		Pedido lido = ler(p);
		assertEquals(StatusPedido.CANCELADO, lido.getStatus());
		assertNotNull(lido.getHistoricoRecente().get(0).getObservacao());
		assertTrue(lido.getHistoricoRecente().get(0).getObservacao().contains("Devolver"));
	}

	@Test
	void pagamentoDesconhecidoOuSemPedidoEhIgnorado() {
		gateway.pagamento = null;
		servico.processarNotificacao(999L);

		gateway.pagamento = new GatewayPagamento.Pagamento(999L, "approved", "EL-000000-ZZZZ", new BigDecimal("10.00"));
		servico.processarNotificacao(999L);
	}
}
