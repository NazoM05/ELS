package com.example.nazomocambique.securityconfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.DadosEntrega;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.ItemCarrinho;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.ProdutoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;
import com.example.nazomocambique.service.PedidoService;

/** Página de conferência do pedido (/admin/separacao/pedido/{codigo}): ADMIN e SEPARADOR, só leitura. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:separacao-detalhes")
@AutoConfigureMockMvc
class SeparacaoDetalhesTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	PedidoService pedidoService;

	@Autowired
	ProdutoRepository produtoRepository;

	@Autowired
	UsuarioRepository usuarioRepository;

	private String codigo;

	@BeforeEach
	void criarPedido() {
		Produto p = new Produto();
		p.setNome("Camiseta Conferência " + System.nanoTime());
		p.setPreco(50.0f);
		p.setTamanho("P,M");
		p.setCategoria(Categoria.CAMISETA);
		p.setGenero(Genero.UNISSEX);
		Map<String, Integer> estoque = new LinkedHashMap<>();
		estoque.put("M", 5);
		p.setEstoque(estoque);
		Long produtoId = produtoRepository.save(p).getId();

		Usuario u = new Usuario();
		u.setUsername("conferencia" + System.nanoTime() + "@teste.com");
		u.setNome("Cliente Conferência");
		u.setRole("ROLE_USUARIO");
		Long usuarioId = usuarioRepository.save(u).getId();

		DadosEntrega d = new DadosEntrega();
		d.setNome("Cliente Conferência");
		d.setTelefone("(51) 99999-9999");
		d.setCep("90000-000");
		d.setEndereco("Rua das Flores");
		d.setNumero("123");
		d.setBairro("Centro");
		d.setCidade("Porto Alegre");
		d.setUf("RS");
		d.setFormaPagamento("PIX");
		Pedido pedido = pedidoService.finalizar(usuarioId, d, List.of(new ItemCarrinho(produtoId, "M", 2)));
		codigo = pedido.getCodigo();
	}

	@Test
	void visitanteSemLoginVaiParaOLogin() throws Exception {
		mvc.perform(get("/admin/separacao/pedido/" + codigo)).andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumNaoAbre() throws Exception {
		mvc.perform(get("/admin/separacao/pedido/" + codigo)).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void separadorVeOsDadosParaConferir() throws Exception {
		String html = mvc.perform(get("/admin/separacao/pedido/" + codigo)).andExpect(status().isOk())
				.andExpect(view().name("admin/pedido/separacao-detalhes")).andReturn().getResponse().getContentAsString();
		assertTrue(html.contains(codigo));
		assertTrue(html.contains("Cliente Conferência"));
		assertTrue(html.contains("(51) 99999-9999"));
		assertTrue(html.contains("Rua das Flores, 123"));
		assertTrue(html.contains("CEP 90000-000"));
		assertTrue(html.contains("Aguardando pagamento"));
		assertTrue(html.contains("Camiseta Conferência"));
		// Só leitura e sem o que é do admin: acompanhamento, histórico e link da ficha completa
		assertFalse(html.contains("Abrir ficha completa do admin"));
		assertFalse(html.contains("/atualizar"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminAbreEViraParaAFichaCompleta() throws Exception {
		String html = mvc.perform(get("/admin/separacao/pedido/" + codigo)).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString();
		assertTrue(html.contains("Abrir ficha completa do admin"));
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void pedidoInexistenteDa404() throws Exception {
		mvc.perform(get("/admin/separacao/pedido/EL-000000-ZZZZ")).andExpect(status().isNotFound());
	}
}
