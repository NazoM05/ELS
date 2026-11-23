package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.DadosEntrega;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.ItemCarrinho;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.ProdutoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;
import com.example.nazomocambique.service.GestaoPedidoService.AtualizacaoPedidoException;

/** Toda venda gera um código de acompanhamento; a equipe acompanha o pedido por ele. */
@SpringBootTest
class GestaoPedidoServiceTests {

	@Autowired
	private PedidoService pedidoService;

	@Autowired
	private GestaoPedidoService gestao;

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Long produtoId;
	private Long usuarioId;

	@BeforeEach
	void preparar() {
		Produto p = new Produto();
		p.setNome("Produto pedidos " + System.nanoTime());
		p.setPreco(50.0f);
		p.setTamanho("P,M");
		p.setCategoria(Categoria.CAMISETA);
		p.setGenero(Genero.UNISSEX);
		Map<String, Integer> estoque = new LinkedHashMap<>();
		estoque.put("P", 2);
		estoque.put("M", 5);
		p.setEstoque(estoque);
		produtoId = produtoRepository.save(p).getId();

		Usuario u = new Usuario();
		u.setUsername("comprador" + System.nanoTime() + "@teste.com");
		u.setNome("Comprador Teste");
		u.setRole("ROLE_USUARIO");
		usuarioId = usuarioRepository.save(u).getId();
	}

	private Pedido vender(int quantidadeM) {
		DadosEntrega d = new DadosEntrega();
		d.setNome("Comprador Teste");
		d.setTelefone("(51) 99999-9999");
		d.setCep("90000-000");
		d.setEndereco("Rua A");
		d.setNumero("1");
		d.setBairro("Centro");
		d.setCidade("Porto Alegre");
		d.setUf("RS");
		d.setFormaPagamento("PIX");
		return pedidoService.finalizar(usuarioId, d, List.of(new ItemCarrinho(produtoId, "M", quantidadeM)));
	}

	private Produto produto() {
		return produtoRepository.findById(produtoId).orElseThrow();
	}

	@Test
	void vendaGeraCodigoUnicoEComecaAguardandoPagamento() {
		Pedido a = vender(1);
		Pedido b = vender(1);

		assertTrue(CodigoPedido.valido(a.getCodigo()), a.getCodigo());
		assertNotEquals(a.getCodigo(), b.getCodigo());
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, a.getStatus());
		Pedido lido = gestao.buscarPorCodigo(a.getCodigo()).orElseThrow();
		assertEquals(1, lido.getHistorico().size());
	}

	@Test
	void equipeAvancaEtapaEFicaRegistradoQuemFez() {
		Pedido p = vender(1);
		assertTrue(gestao.atualizar(p.getCodigo(), StatusPedido.ENVIADO, "aa123456789br", "Saiu hoje", "Ana"));

		Pedido lido = gestao.buscarPorCodigo(p.getCodigo()).orElseThrow();
		assertEquals(StatusPedido.ENVIADO, lido.getStatus());
		assertEquals("AA123456789BR", lido.getCodigoRastreio());
		assertEquals(2, lido.getHistorico().size());
		assertEquals("Ana", lido.getHistoricoRecente().get(0).getResponsavel());
		assertTrue(lido.getHistoricoRecente().get(0).getObservacao().contains("Saiu hoje"));
	}

	@Test
	void salvarSemMudarNadaNaoCriaHistorico() {
		Pedido p = vender(1);
		assertFalse(gestao.atualizar(p.getCodigo(), StatusPedido.AGUARDANDO_PAGAMENTO, "", "  ", "Ana"));
		assertEquals(1, gestao.buscarPorCodigo(p.getCodigo()).orElseThrow().getHistorico().size());
	}

	@Test
	void cancelarDevolveOEstoqueUmaVezSoEPedidoNaoReabre() {
		Pedido p = vender(3);
		assertEquals(2, produto().getEstoque().get("M"));

		gestao.atualizar(p.getCodigo(), StatusPedido.CANCELADO, null, "Cliente desistiu", "Ana");
		assertEquals(5, produto().getEstoque().get("M"));
		assertEquals(0, produto().getVendidasTotal());

		// salvar de novo como cancelado (ex.: só uma observação) não devolve outra vez
		gestao.atualizar(p.getCodigo(), StatusPedido.CANCELADO, null, "Reembolso feito", "Ana");
		assertEquals(5, produto().getEstoque().get("M"));

		assertThrows(AtualizacaoPedidoException.class,
				() -> gestao.atualizar(p.getCodigo(), StatusPedido.PAGO, null, null, "Ana"));
	}

	@Test
	void rastreioComCaracteresEstranhosERecusado() {
		Pedido p = vender(1);
		assertThrows(AtualizacaoPedidoException.class,
				() -> gestao.atualizar(p.getCodigo(), StatusPedido.ENVIADO, "<script>", null, "Ana"));
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, gestao.buscarPorCodigo(p.getCodigo()).orElseThrow().getStatus());
	}

	@Test
	void buscaAceitaCodigoDigitadoEmMinusculas() {
		Pedido p = vender(1);
		assertTrue(gestao.buscarPorCodigo(p.getCodigo().toLowerCase().replace("-", "")).isPresent());
	}

	// ------------------------------------------------------------ histórico de alterações

	@Test
	void mudancaDeEtapaGuardaEtapaAnteriorENovaQuemEQuando() {
		Pedido p = vender(1);
		java.time.LocalDateTime antes = java.time.LocalDateTime.now().minusSeconds(1);
		gestao.atualizar(p.getCodigo(), StatusPedido.PAGO, null, null, "Ana", "ana@loja.com");
		gestao.atualizar(p.getCodigo(), StatusPedido.EM_SEPARACAO, null, null, "Bruno", "bruno@loja.com");

		var recente = gestao.buscarPorCodigo(p.getCodigo()).orElseThrow().getHistoricoRecente();
		var ultima = recente.get(0);
		assertEquals(StatusPedido.PAGO, ultima.getStatusAnterior());
		assertEquals(StatusPedido.EM_SEPARACAO, ultima.getStatus());
		assertEquals("Bruno", ultima.getResponsavel());
		assertEquals("bruno@loja.com", ultima.getUsuarioLogin());
		assertTrue(!ultima.getCriadoEm().isBefore(antes));

		var penultima = recente.get(1);
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, penultima.getStatusAnterior());
		assertEquals(StatusPedido.PAGO, penultima.getStatus());
		assertEquals("ana@loja.com", penultima.getUsuarioLogin());
	}

	@Test
	void mudancaDeRastreioGuardaValorAnteriorENovo() {
		Pedido p = vender(1);
		gestao.atualizar(p.getCodigo(), StatusPedido.ENVIADO, "AA111111111BR", null, "Ana", "ana@loja.com");
		gestao.atualizar(p.getCodigo(), StatusPedido.ENVIADO, "BB222222222BR", null, "Ana", "ana@loja.com");

		var ultima = gestao.buscarPorCodigo(p.getCodigo()).orElseThrow().getHistoricoRecente().get(0);
		assertTrue(ultima.isAlterouRastreio());
		assertEquals("AA111111111BR", ultima.getRastreioAnterior());
		assertEquals("BB222222222BR", ultima.getRastreioNovo());
		assertFalse(ultima.isMudouEtapa());
	}

	@Test
	void criacaoDoPedidoNaoTemEtapaAnteriorEGuardaOLoginDoCliente() {
		Pedido p = vender(1);
		var criacao = gestao.buscarPorCodigo(p.getCodigo()).orElseThrow().getHistorico().get(0);
		assertEquals(null, criacao.getStatusAnterior());
		assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, criacao.getStatus());
		assertEquals(usuarioRepository.findById(usuarioId).orElseThrow().getUsername(), criacao.getUsuarioLogin());
	}
}
