package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Cotacao;
import com.example.nazomocambique.model.DadosEntrega;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.ItemCarrinho;
import com.example.nazomocambique.model.Pacote;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.PrePostagem;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.model.SolicitacaoPostagem;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.repository.ProdutoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;
import com.example.nazomocambique.service.EtiquetaEnvioService.EtiquetaException;

/** Etiqueta de envio: serviço (pré-postagem) e telas, com os Correios substituídos por uma versão falsa. */
@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:etiqueta-envio", "correios.enabled=true",
		"correios.usuario=12345678000199", "correios.codigo-acesso=codigo", "correios.cartao-postagem=0070123456",
		"correios.cep-origem=90010000", "correios.remetente.nome=Loja Teste", "correios.remetente.cpf-cnpj=12345678000199",
		"correios.remetente.logradouro=Rua da Loja", "correios.remetente.numero=10", "correios.remetente.bairro=Centro",
		"correios.remetente.cidade=Porto Alegre", "correios.remetente.uf=RS" })
@AutoConfigureMockMvc
class EtiquetaEnvioTests {

	/** Correios de mentirinha: cota frete e devolve um código fixo (só nos testes; o sistema real nunca inventa). */
	static class CorreiosFalso implements CotacaoCorreios, PostagemCorreios {
		final AtomicInteger postagens = new AtomicInteger();
		volatile String codigoObjeto = "AA123456789BR";

		@Override
		public Cotacao cotar(String codigoServico, String cepDestino, Pacote pacote) {
			return new Cotacao(new BigDecimal("20.00"), 5);
		}

		@Override
		public PrePostagem criarPrePostagem(SolicitacaoPostagem s) {
			postagens.incrementAndGet();
			return new PrePostagem("PR-TESTE-1", codigoObjeto);
		}
	}

	@TestConfiguration
	static class Config {
		@Bean
		@Primary
		CorreiosFalso correiosFalso() {
			return new CorreiosFalso();
		}
	}

	@Autowired
	MockMvc mvc;
	@Autowired
	CorreiosFalso falso;
	@Autowired
	EtiquetaEnvioService etiquetaService;
	@Autowired
	GestaoPedidoService gestao;
	@Autowired
	PedidoService pedidoService;
	@Autowired
	PedidoRepository pedidoRepository;
	@Autowired
	ProdutoRepository produtoRepository;
	@Autowired
	UsuarioRepository usuarioRepository;

	private String codigo;

	@BeforeEach
	void criarPedido() {
		falso.postagens.set(0);
		falso.codigoObjeto = "AA123456789BR";

		Produto p = new Produto();
		p.setNome("Camiseta Etiqueta " + System.nanoTime());
		p.setPreco(50.0f);
		p.setTamanho("P,M");
		p.setCategoria(Categoria.CAMISETA);
		p.setGenero(Genero.UNISSEX);
		Map<String, Integer> estoque = new LinkedHashMap<>();
		estoque.put("M", 5);
		p.setEstoque(estoque);
		Long produtoId = produtoRepository.save(p).getId();

		Usuario u = new Usuario();
		u.setUsername("etiqueta" + System.nanoTime() + "@teste.com");
		u.setNome("Cliente Etiqueta");
		u.setRole("ROLE_USUARIO");
		Long usuarioId = usuarioRepository.save(u).getId();

		DadosEntrega d = new DadosEntrega();
		d.setNome("Cliente Etiqueta");
		d.setTelefone("(51) 99999-9999");
		d.setCep("01310-100");
		d.setEndereco("Avenida Paulista");
		d.setNumero("1000");
		d.setBairro("Bela Vista");
		d.setCidade("São Paulo");
		d.setUf("SP");
		d.setFormaPagamento("PIX");
		Pedido pedido = pedidoService.finalizar(usuarioId, d, List.of(new ItemCarrinho(produtoId, "M", 2)));
		codigo = pedido.getCodigo();
	}

	private void confirmarPagamento() {
		gestao.atualizar(codigo, StatusPedido.PAGO, "", "", "Teste");
	}

	// ---------------------------------------------------------------- serviço

	@Test
	void pedidoUsaServicoDosCorreiosEscolhidoNoCheckout() {
		Pedido p = gestao.buscarPorCodigo(codigo).orElseThrow();
		assertEquals("03298", EtiquetaEnvioService.codigoServico(p));
	}

	@Test
	void naoGeraEnquantoOPagamentoNaoForConfirmado() {
		assertThrows(EtiquetaException.class, () -> etiquetaService.gerar(codigo, "Teste", "teste"));
		assertEquals(0, falso.postagens.get());
	}

	@Test
	void geraGuardaOCodigoDosCorreiosENaoMexeNoRastreioDoCliente() {
		confirmarPagamento();
		PrePostagem r = etiquetaService.gerar(codigo, "Teste", "teste");
		assertEquals("AA123456789BR", r.codigoObjeto());

		Pedido p = gestao.buscarPorCodigo(codigo).orElseThrow();
		assertTrue(p.isEtiquetaGerada());
		assertEquals("PR-TESTE-1", p.getCorreiosIdPrePostagem());
		assertNull(p.getCodigoRastreio(), "o cliente só vê o rastreio quando o pedido é enviado");
		assertEquals(StatusPedido.PAGO, p.getStatus());
	}

	@Test
	void gerarDeNovoNaoCriaOutraPrePostagem() {
		confirmarPagamento();
		etiquetaService.gerar(codigo, "Teste", "teste");
		etiquetaService.gerar(codigo, "Teste", "teste");
		assertEquals(1, falso.postagens.get());
	}

	@Test
	void entregaPadraoDaLojaNaoTemEtiquetaDosCorreios() {
		confirmarPagamento();
		Pedido p = pedidoRepository.findByCodigo(codigo).orElseThrow();
		p.setServicoFreteCodigo("PADRAO");
		p.setServicoFrete("Entrega padrão");
		pedidoRepository.save(p);

		assertThrows(EtiquetaException.class, () -> etiquetaService.gerar(codigo, "Teste", "teste"));
		assertEquals(0, falso.postagens.get());
	}

	@Test
	void codigoForaDoPadraoNaoEGravado() {
		confirmarPagamento();
		falso.codigoObjeto = "INVENTADO";
		assertThrows(EtiquetaException.class, () -> etiquetaService.gerar(codigo, "Teste", "teste"));
		assertFalse(gestao.buscarPorCodigo(codigo).orElseThrow().isEtiquetaGerada());
	}

	// ---------------------------------------------------------------- telas

	@Test
	void visitanteSemLoginVaiParaOLogin() throws Exception {
		mvc.perform(get("/admin/separacao/pedido/" + codigo + "/etiqueta")).andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumNaoGeraNemImprime() throws Exception {
		mvc.perform(post("/admin/separacao/pedido/" + codigo + "/etiqueta").with(csrf()))
				.andExpect(status().isForbidden());
		mvc.perform(get("/admin/separacao/pedido/" + codigo + "/etiqueta")).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void gerarSemTokenCsrfEBarrado() throws Exception {
		mvc.perform(post("/admin/separacao/pedido/" + codigo + "/etiqueta")).andExpect(status().isForbidden());
		assertEquals(0, falso.postagens.get());
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void separadorGeraEImprimeNasDuasMedidas() throws Exception {
		confirmarPagamento();
		mvc.perform(post("/admin/separacao/pedido/" + codigo + "/etiqueta").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attributeExists("sucesso"));

		String termica = mvc.perform(get("/admin/separacao/pedido/" + codigo + "/etiqueta")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertTrue(termica.contains("AA123456789BR"));
		assertTrue(termica.contains("<svg"));
		assertTrue(termica.contains("CEP 01310-100"));
		assertTrue(termica.contains("Cliente Etiqueta"));
		assertTrue(termica.contains("Loja Teste"));
		assertTrue(termica.contains("CEP 90010-000"));
		assertTrue(termica.contains("PAC"));
		assertTrue(termica.contains("pg-etiqueta-termica"));
		assertFalse(termica.contains("12345678000199"), "CPF/CNPJ e credenciais não vão para a etiqueta");

		String a4 = mvc.perform(get("/admin/separacao/pedido/" + codigo + "/etiqueta").param("formato", "a4"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertTrue(a4.contains("pg-etiqueta-a4"));
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void semEtiquetaGeradaImprimirVoltaParaOPedido() throws Exception {
		mvc.perform(get("/admin/separacao/pedido/" + codigo + "/etiqueta")).andExpect(status().is3xxRedirection())
				.andExpect(flash().attributeExists("erro"));
	}
}
