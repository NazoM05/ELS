package com.example.nazomocambique.securityconfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Regra: carteira, checkout e pagamento de pedidos não são do SEPARADOR; ADMIN e cliente seguem como antes. */
// Banco próprio: o data.sql roda de novo neste contexto (usuários "admin", "separador" e "user")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:carteira-acesso")
@AutoConfigureMockMvc
class CarteiraAcessoTests {

	@Autowired
	MockMvc mvc;

	// ------------------------------------------------------------ separador: bloqueado

	@Test
	@WithMockUser(username = "separador", roles = "SEPARADOR")
	void separadorNaoAbreACarteira() throws Exception {
		mvc.perform(get("/carteira")).andExpect(redirectedUrl("/acesso-negado"));
		mvc.perform(get("/carteira/")).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(username = "separador", roles = "SEPARADOR")
	void separadorNaoUsaOCheckout() throws Exception {
		mvc.perform(get("/checkout")).andExpect(redirectedUrl("/acesso-negado"));
		mvc.perform(post("/checkout").param("item", "1:M:1")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "separador", roles = "SEPARADOR")
	void separadorNaoVePedidoNemPagamentoDeCliente() throws Exception {
		mvc.perform(get("/pedido/1")).andExpect(redirectedUrl("/acesso-negado"));
		mvc.perform(post("/pedido/1/pix")).andExpect(status().isForbidden());
		mvc.perform(get("/pedido/1/status").accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "separador", roles = "SEPARADOR")
	void separadorNaoVeOLinkMinhaCarteiraNoMenu() throws Exception {
		// A tela de separação dele continua funcionando, sem nenhuma pista da carteira
		mvc.perform(get("/admin/separacao")).andExpect(status().isOk());
		String quadro = mvc.perform(get("/admin/separacao")).andReturn().getResponse().getContentAsString();
		assertFalse(quadro.contains("Minha carteira"));
		assertFalse(quadro.contains("/carteira"));
	}

	// ------------------------------------------------------------ sem login

	@Test
	void visitanteSemLoginContinuaIndoParaOLogin() throws Exception {
		mvc.perform(get("/carteira")).andExpect(redirectedUrl("/login"));
		mvc.perform(get("/checkout")).andExpect(redirectedUrl("/login"));
		mvc.perform(get("/pedido/1")).andExpect(redirectedUrl("/login"));
	}

	// ------------------------------------------------------------ perfis autorizados: preservados

	@Test
	@WithMockUser(username = "user", roles = "USUARIO")
	void clienteAbreACarteiraEVeOLinkNoMenu() throws Exception {
		mvc.perform(get("/carteira")).andExpect(status().isOk()).andExpect(view().name("carteira"));
		String html = mvc.perform(get("/home")).andReturn().getResponse().getContentAsString();
		assertTrue(html.contains("Minha carteira"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminContinuaComACarteira() throws Exception {
		mvc.perform(get("/carteira")).andExpect(status().isOk()).andExpect(view().name("carteira"));
		String html = mvc.perform(get("/admin/separacao")).andReturn().getResponse().getContentAsString();
		assertTrue(html.contains("Minha carteira"));
	}
}
