package com.example.nazomocambique.securityconfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.UsuarioRepository;

/** Regra: a gestão de usuários é só do ADMIN; desativar exige token CSRF e derruba a sessão de quem foi desativado. */
// Banco próprio: o data.sql roda de novo neste contexto (usuários "admin", "gestor" e "user")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:usuarios-acesso")
@AutoConfigureMockMvc
class AdminUsuarioAcessoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	UsuarioRepository usuarioRepository;

	// ------------------------------------------------------------ quem pode abrir

	@Test
	void visitanteSemLoginEhMandadoParaOLogin() throws Exception {
		mvc.perform(get("/admin/usuarios")).andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumVaiParaAcessoNegado() throws Exception {
		mvc.perform(get("/admin/usuarios")).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(roles = "GESTOR")
	void gestorVaiParaAcessoNegado() throws Exception {
		mvc.perform(get("/admin/usuarios")).andExpect(redirectedUrl("/acesso-negado"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminAbreAListaEAFicha() throws Exception {
		mvc.perform(get("/admin/usuarios")).andExpect(status().isOk()).andExpect(view().name("admin/usuario/listar"));

		Usuario cliente = usuarioRepository.findByUsername("user").orElseThrow();
		mvc.perform(get("/admin/usuarios/" + cliente.getId())).andExpect(status().isOk())
				.andExpect(view().name("admin/usuario/detalhes"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void fichaDeUsuarioInexistenteDaNaoEncontrado() throws Exception {
		mvc.perform(get("/admin/usuarios/999999")).andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------ ativar / desativar

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void desativarSemTokenCsrfEhBarrado() throws Exception {
		Usuario cliente = usuarioRepository.findByUsername("user").orElseThrow();
		mvc.perform(post("/admin/usuarios/" + cliente.getId() + "/status").param("ativo", "false"))
				.andExpect(status().isForbidden());
		assertTrue(usuarioRepository.findById(cliente.getId()).orElseThrow().getAtivo());
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void clienteComumNaoDesativaNinguem() throws Exception {
		Usuario cliente = usuarioRepository.findByUsername("user").orElseThrow();
		mvc.perform(post("/admin/usuarios/" + cliente.getId() + "/status").param("ativo", "false").with(csrf()))
				.andExpect(status().isForbidden());
		assertTrue(usuarioRepository.findById(cliente.getId()).orElseThrow().getAtivo());
	}

	@Test
	@Transactional
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminDesativaEReativaUmCliente() throws Exception {
		Usuario cliente = usuarioRepository.findByUsername("user").orElseThrow();

		mvc.perform(post("/admin/usuarios/" + cliente.getId() + "/status").param("ativo", "false").with(csrf()))
				.andExpect(redirectedUrl("/admin/usuarios")).andExpect(flash().attribute("mensagemOk", "Conta desativada."));
		assertFalse(usuarioRepository.findById(cliente.getId()).orElseThrow().getAtivo());

		mvc.perform(post("/admin/usuarios/" + cliente.getId() + "/status").param("ativo", "true")
				.param("retorno", "detalhe").with(csrf()))
				.andExpect(redirectedUrl("/admin/usuarios/" + cliente.getId()))
				.andExpect(flash().attribute("mensagemOk", "Conta ativada."));
		assertTrue(usuarioRepository.findById(cliente.getId()).orElseThrow().getAtivo());
	}

	@Test
	@Transactional
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminNaoDesativaAPropriaConta() throws Exception {
		Usuario admin = usuarioRepository.findByUsername("admin").orElseThrow();

		mvc.perform(post("/admin/usuarios/" + admin.getId() + "/status").param("ativo", "false").with(csrf()))
				.andExpect(redirectedUrl("/admin/usuarios")).andExpect(flash().attributeExists("erro"));
		assertTrue(usuarioRepository.findById(admin.getId()).orElseThrow().getAtivo());
	}

	// ------------------------------------------------------------ sessão aberta de conta desativada

	@Test
	@Transactional
	void contaDesativadaComSessaoAbertaEhDesconectada() throws Exception {
		Usuario inativo = new Usuario();
		inativo.setUsername("inativo-com-sessao@teste.com");
		inativo.setRole("ROLE_ADMIN");
		inativo.setAtivo(false);
		usuarioRepository.save(inativo);

		// Mesmo com perfil de admin, quem foi desativado é mandado para o login
		mvc.perform(get("/admin/usuarios").with(user("inativo-com-sessao@teste.com").roles("ADMIN")))
				.andExpect(redirectedUrl("/login?desativada=true"));
	}
}
