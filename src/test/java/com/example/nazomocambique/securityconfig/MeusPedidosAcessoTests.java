package com.example.nazomocambique.securityconfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** "Meus pedidos" (área do usuário): só ADMIN e cliente entram; visitante vai ao login; separador é barrado. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:meus-pedidos-acesso")
@AutoConfigureMockMvc
class MeusPedidosAcessoTests {

	@Autowired
	MockMvc mvc;

	@Test
	void visitanteVaiParaOLogin() throws Exception {
		mvc.perform(get("/meus-pedidos")).andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(username = "separador", roles = "SEPARADOR")
	void separadorNaoAbreNemVeOLinkNoMenu() throws Exception {
		mvc.perform(get("/meus-pedidos")).andExpect(redirectedUrl("/acesso-negado"));
		String quadro = mvc.perform(get("/admin/separacao")).andReturn().getResponse().getContentAsString();
		assertFalse(quadro.contains("/meus-pedidos"));
		assertFalse(quadro.contains("Área do usuário"));
	}

	@Test
	@WithMockUser(username = "user", roles = "USUARIO")
	void clienteAbreAListaEVeAAreaDoUsuarioNoMenu() throws Exception {
		mvc.perform(get("/meus-pedidos")).andExpect(status().isOk()).andExpect(view().name("meus-pedidos"));
		mvc.perform(get("/meus-pedidos").param("filtro", "andamento")).andExpect(status().isOk());
		mvc.perform(get("/meus-pedidos").param("filtro", "qualquer-coisa")).andExpect(status().isOk());
		String html = mvc.perform(get("/home")).andReturn().getResponse().getContentAsString();
		assertTrue(html.contains("Área do usuário"));
		assertTrue(html.contains("/meus-pedidos"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminTambemTemSuaListaDePedidos() throws Exception {
		mvc.perform(get("/meus-pedidos")).andExpect(status().isOk()).andExpect(view().name("meus-pedidos"));
	}
}
