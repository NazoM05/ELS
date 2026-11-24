package com.example.nazomocambique.securityconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.UsuarioRepository;
import com.example.nazomocambique.service.ContaService;

/** Regra: a separação de pedidos (tela e dados) é só para ADMIN e SEPARADOR; todo o resto é bloqueado. */
// Banco próprio: o data.sql roda de novo neste contexto e duplicaria os usuários do banco "nmb"
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:separacao-acesso")
@AutoConfigureMockMvc
class SeparacaoAcessoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	UsuarioRepository usuarioRepository;

	@Autowired
	ContaService contaService;

	// ------------------------------------------------------------ sem login

	@Test
	void visitanteSemLoginEhMandadoParaOLogin() throws Exception {
		mvc.perform(get("/admin/separacao")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void visitanteSemLoginNaoLeOsPedidosDaSeparacao() throws Exception {
		mvc.perform(get("/admin/separacao/pedidos").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
	}

	@Test
	void visitanteSemLoginNaoMudaEtapa() throws Exception {
		mvc.perform(post("/admin/separacao/EL-1/status").param("status", "ENVIADO").with(csrf()))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
	}

	// ------------------------------------------------------------ cliente comum

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumVaiParaAcessoNegado() throws Exception {
		mvc.perform(get("/admin/separacao")).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumNaoLeOsPedidosDaSeparacao() throws Exception {
		mvc.perform(get("/admin/separacao/pedidos").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumNaoMudaEtapa() throws Exception {
		mvc.perform(post("/admin/separacao/EL-1/status").param("status", "ENVIADO").with(csrf()))
				.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------ separador

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void separadorAbreATelaDeSeparacao() throws Exception {
		mvc.perform(get("/admin/separacao")).andExpect(status().isOk())
				.andExpect(view().name("admin/pedido/separacao"));
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void separadorLeOsPedidosDaSeparacao() throws Exception {
		mvc.perform(get("/admin/separacao/pedidos").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void separadorPassaPelaSegurancaAoMudarEtapa() throws Exception {
		// Etapa inexistente: chega no controller (400) em vez de ser barrado (403)
		mvc.perform(post("/admin/separacao/EL-1/status").param("status", "INVALIDA").with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void separadorNaoEntraNasOutrasAreasDoAdmin() throws Exception {
		mvc.perform(get("/admin/pedidos")).andExpect(redirectedUrl("/acesso-negado"));
		mvc.perform(get("/admin/produto/listar")).andExpect(redirectedUrl("/acesso-negado"));
		mvc.perform(get("/admin/configuracoes")).andExpect(redirectedUrl("/acesso-negado"));
	}

	// ------------------------------------------------------------ admin

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminAbreATelaDeSeparacao() throws Exception {
		mvc.perform(get("/admin/separacao")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminLeOsPedidosDaSeparacao() throws Exception {
		mvc.perform(get("/admin/separacao/pedidos").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	// ------------------------------------------------------------ CSRF

	@Test
	@WithMockUser(roles = "ADMIN")
	void mudarEtapaSemTokenDaPaginaEhBloqueado() throws Exception {
		mvc.perform(post("/admin/separacao/EL-1/status").param("status", "ENVIADO"))
				.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------ contas

	@Test
	void dadosIniciaisTemUmSeparadorComPerfilProprio() {
		Usuario separador = usuarioRepository.findByUsername("separador").orElseThrow();
		assertEquals("ROLE_SEPARADOR", separador.getRole());
		assertTrue(contaService.loadUserByUsername("separador").isEnabled());
	}

	@Test
	@Transactional
	void contaDesativadaNaoConsegueEntrar() {
		Usuario u = new Usuario();
		u.setUsername("separador-inativo@teste.com");
		u.setRole("ROLE_SEPARADOR");
		u.setAtivo(false);
		usuarioRepository.save(u);
		assertFalse(contaService.loadUserByUsername("separador-inativo@teste.com").isEnabled());
	}

	// ------------------------------------------------------------ histórico de alterações (só admin)

	@Test
	void visitanteSemLoginNaoVeOHistorico() throws Exception {
		mvc.perform(get("/admin/historico")).andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumNaoVeOHistorico() throws Exception {
		mvc.perform(get("/admin/historico")).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(roles = "SEPARADOR")
	void separadorNaoVeOHistorico() throws Exception {
		mvc.perform(get("/admin/historico")).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminVeOHistorico() throws Exception {
		mvc.perform(get("/admin/historico")).andExpect(status().isOk()).andExpect(view().name("admin/historico"));
	}
}

