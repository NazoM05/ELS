package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
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
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.ProdutoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;

/** Regras da finalização da compra: baixa do estoque por tamanho e bloqueio quando falta estoque. */
@SpringBootTest
class PedidoServiceTests {

	@Autowired
	private PedidoService pedidoService;

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Long produtoId;
	private Long usuarioId;

	@BeforeEach
	void preparar() {
		Produto p = new Produto();
		p.setNome("Produto teste " + System.nanoTime());
		p.setPreco(60.0f);
		p.setTamanho("P,M,Único");
		p.setCategoria(Categoria.CAMISETA);
		p.setGenero(Genero.UNISSEX);
		Map<String, Integer> estoque = new LinkedHashMap<>();
		estoque.put("P", 1);
		estoque.put("M", 3);
		estoque.put("Único", 2);
		p.setEstoque(estoque);
		produtoId = produtoRepository.save(p).getId();

		Usuario u = new Usuario();
		u.setUsername("cliente" + System.nanoTime() + "@teste.com");
		u.setNome("Cliente Teste");
		u.setRole("ROLE_USUARIO");
		usuarioId = usuarioRepository.save(u).getId();
	}

	private DadosEntrega entrega() {
		DadosEntrega d = new DadosEntrega();
		d.setNome("Cliente Teste");
		d.setTelefone("(51) 99999-9999");
		d.setCep("90000-000");
		d.setEndereco("Rua das Flores");
		d.setNumero("123");
		d.setBairro("Centro");
		d.setCidade("Porto Alegre");
		d.setUf("RS");
		d.setFormaPagamento("PIX");
		return d;
	}

	private Produto recarregar() {
		return produtoRepository.findById(produtoId).orElseThrow();
	}

	@Test
	void desconta_o_estoque_de_cada_tamanho_vendido() {
		pedidoService.finalizar(usuarioId, entrega(),
				List.of(new ItemCarrinho(produtoId, "M", 2), new ItemCarrinho(produtoId, "P", 1)));

		Produto p = recarregar();
		assertEquals(1, p.getEstoque().get("M"));
		assertEquals(0, p.getEstoque().get("P"));
		assertEquals(2, p.getEstoque().get("Único"));
		assertEquals(3, p.getVendidasTotal());
	}

	@Test
	void tamanho_em_maiusculas_do_carrinho_baixa_o_tamanho_cadastrado() {
		pedidoService.finalizar(usuarioId, entrega(), List.of(new ItemCarrinho(produtoId, "ÚNICO", 1)));

		assertEquals(1, recarregar().getEstoque().get("Único"));
	}

	@Test
	void sem_estoque_suficiente_nao_finaliza_e_nao_altera_nenhum_tamanho() {
		assertThrows(CheckoutException.class, () -> pedidoService.finalizar(usuarioId, entrega(),
				List.of(new ItemCarrinho(produtoId, "M", 1), new ItemCarrinho(produtoId, "P", 2))));

		Produto p = recarregar();
		assertEquals(3, p.getEstoque().get("M"));
		assertEquals(1, p.getEstoque().get("P"));
		assertEquals(0, p.getVendidasTotal());
	}

	private DadosEntrega entregaEm(String uf) {
		DadosEntrega d = entrega();
		d.setUf(uf);
		return d;
	}

	@Test
	void preco_vem_do_banco_e_sul_tem_frete_gratis_a_partir_de_99() {
		Pedido pedido = pedidoService.finalizar(usuarioId, entregaEm("RS"), List.of(new ItemCarrinho(produtoId, "M", 2)));

		assertEquals(new BigDecimal("120.00"), pedido.getSubtotal());
		assertEquals(0, pedido.getFrete().signum());
		assertEquals(new BigDecimal("120.00"), pedido.getTotal());
	}

	@Test
	void sul_abaixo_de_99_cobra_frete_de_19_90() {
		Pedido pedido = pedidoService.finalizar(usuarioId, entregaEm("SC"), List.of(new ItemCarrinho(produtoId, "M", 1)));

		assertEquals(new BigDecimal("19.90"), pedido.getFrete());
		assertEquals(new BigDecimal("79.90"), pedido.getTotal());
	}

	@Test
	void fora_do_sul_120_reais_ainda_paga_frete() {
		Pedido pedido = pedidoService.finalizar(usuarioId, entregaEm("SP"), List.of(new ItemCarrinho(produtoId, "M", 2)));

		assertEquals(new BigDecimal("19.90"), pedido.getFrete());
		assertEquals(new BigDecimal("139.90"), pedido.getTotal());
	}

	@Test
	void fora_do_sul_frete_gratis_a_partir_de_250() {
		Pedido pedido = pedidoService.finalizar(usuarioId, entregaEm("BA"),
				List.of(new ItemCarrinho(produtoId, "M", 3), new ItemCarrinho(produtoId, "Único", 2)));

		assertEquals(new BigDecimal("300.00"), pedido.getSubtotal());
		assertEquals(0, pedido.getFrete().signum());
	}

	@Test
	void limites_de_frete_gratis_por_regiao() {
		assertEquals(0, PedidoService.calcularFrete(new BigDecimal("99.00"), "PR").signum());
		assertEquals(new BigDecimal("19.90"), PedidoService.calcularFrete(new BigDecimal("249.99"), "RJ"));
		assertEquals(0, PedidoService.calcularFrete(new BigDecimal("250.00"), "RJ").signum());
	}

	@Test
	void salvar_dados_marcado_guarda_endereco_no_cadastro() {
		pedidoService.finalizar(usuarioId, entrega(), List.of(new ItemCarrinho(produtoId, "M", 1)));
		Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
		assertEquals("Rua das Flores", u.getEndereco());
		assertEquals("90000000", u.getCep());
	}

	@Test
	void salvar_dados_desmarcado_nao_altera_o_cadastro() {
		DadosEntrega d = entrega();
		d.setSalvarDados(false);
		pedidoService.finalizar(usuarioId, d, List.of(new ItemCarrinho(produtoId, "M", 1)));
		Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
		assertEquals(null, u.getEndereco());
		assertEquals(null, u.getCep());
	}

	@Test
	void cpf_de_outra_conta_e_recusado_com_erro_no_campo() {
		// O CPF é único no banco: só cria o "outro" cliente se ainda não existir (o banco de testes pode ser reaproveitado).
		if (!usuarioRepository.existsByCpfAndIdNot("85369144087", -1L)) {
			Usuario outro = new Usuario();
			outro.setUsername("outro" + System.nanoTime() + "@teste.com");
			outro.setNome("Outro Cliente");
			outro.setRole("ROLE_USUARIO");
			outro.setCpf("85369144087");
			usuarioRepository.save(outro);
		}
		DadosEntrega d = entrega();
		d.setCpf("853.691.440-87");
		CheckoutException e = assertThrows(CheckoutException.class,
				() -> pedidoService.finalizar(usuarioId, d, List.of(new ItemCarrinho(produtoId, "M", 1))));
		assertEquals(true, e.getErrosCampos().containsKey("cpf"));
	}
}
