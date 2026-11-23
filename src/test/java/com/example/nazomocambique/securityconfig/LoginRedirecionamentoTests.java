package com.example.nazomocambique.securityconfig;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.UsuarioRepository;

/** Depois do login o gestor vai direto para a separação de pedidos; os outros perfis seguem para o início. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:login-redirect")
@AutoConfigureMockMvc
@Transactional
class LoginRedirecionamentoTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	UsuarioRepository usuarioRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	private void criar(String email, String role) {
		Usuario u = new Usuario();
		u.setUsername(email);
		u.setSenha(passwordEncoder.encode("Senha123"));
		u.setRole(role);
		u.setAtivo(true);
		usuarioRepository.save(u);
	}

	@Test
	void gestorEntraDiretoNaSeparacao() throws Exception {
		criar("gestor-login@teste.com", "ROLE_GESTOR");
		mvc.perform(formLogin("/perform_login").user("gestor-login@teste.com").password("Senha123"))
				.andExpect(redirectedUrl("/admin/separacao"));
	}

	@Test
	void adminEClienteSeguemParaOInicio() throws Exception {
		criar("admin-login@teste.com", "ROLE_ADMIN");
		criar("cliente-login@teste.com", "ROLE_USUARIO");
		mvc.perform(formLogin("/perform_login").user("admin-login@teste.com").password("Senha123"))
				.andExpect(redirectedUrl("/"));
		mvc.perform(formLogin("/perform_login").user("cliente-login@teste.com").password("Senha123"))
				.andExpect(redirectedUrl("/"));
	}
}
