package com.example.nazomocambique.securityconfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** As páginas de coleção são públicas: quem não está logado abre todas sem ser mandado para o login. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:colecao-publica")
@AutoConfigureMockMvc
class ColecaoAcessoPublicoTests {

	@Autowired
	MockMvc mvc;

	@Test
	void visitanteAbreAsTresColecoesSemLogin() throws Exception {
		for (String caminho : new String[] { "/colecao/vice-city", "/colecao/tricolor", "/colecao/urban-culture" }) {
			mvc.perform(get(caminho)).andExpect(status().isOk());
		}
	}

	@Test
	void colecoesTrazSoOsProdutosDelasEMenuComAsNovasAbas() throws Exception {
		mvc.perform(get("/colecao/tricolor")).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Manto Imortal Tricolor II")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Camiseta Neon Sunset"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/colecao/urban-culture")));
		mvc.perform(get("/colecao/urban-culture")).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Urban Culture Preta")));
	}

	@Test
	void carrosselDeCampanhasNaoApareceNasColecoes() throws Exception {
		mvc.perform(get("/colecao/tricolor")).andExpect(content()
				.string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-promo-carrossel"))));
	}
}
