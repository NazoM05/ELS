package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.nazomocambique.model.Aparencia;
import com.example.nazomocambique.model.CatalogoPersonalizacao;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Secao;

class PersonalizacaoServiceTests {

	@TempDir
	Path pasta;

	private PersonalizacaoService novo() {
		PersonalizacaoService s = new PersonalizacaoService(pasta.resolve("personalizacao.properties"));
		s.carregar();
		return s;
	}

	private static Secao secao(String id) {
		return CatalogoPersonalizacao.secao(id);
	}

	@Test
	void semNadaSalvoUsaOsTextosPadrao() {
		PersonalizacaoService s = novo();
		assertEquals("Estilo London Store", s.texto("geral.nomeLoja"));
		assertEquals("Comprar agora", s.texto("pg.detalhes.btnComprar"));
		assertEquals("", s.css(Aparencia.padrao()));
	}

	@Test
	void nomeDaPaginaEBotaoPersonalizadosSobrevivemAoReinicio() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("pg-login"), Map.of("pg.login.titulo", "Acessar", "pg.login.btnEntrar", "Bora"));

		PersonalizacaoService depois = novo();
		assertEquals("Acessar", depois.texto("pg.login.titulo"));
		assertEquals("Bora", depois.texto("pg.login.btnEntrar"));
	}

	@Test
	void textoObrigatorioVazioVoltaAoPadrao() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("pg-login"), Map.of("pg.login.btnEntrar", "Bora"));
		s.salvarSecao(secao("pg-login"), Map.of("pg.login.btnEntrar", "   "));
		assertEquals("Entrar", s.texto("pg.login.btnEntrar"));
		assertNull(s.salvo("pg.login.btnEntrar"));
	}

	@Test
	void avisoOpcionalEmBrancoSomeDaBarra() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("textos"), Map.of("geral.anuncio2", ""));
		assertEquals(3, s.anuncios().size());
		assertFalse(s.anuncios().contains("Novas estampas disponíveis"));
	}

	@Test
	void chaveDeOutraSecaoOuForaDoCatalogoNaoEGravada() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("pg-login"), Map.of("pg.perfil.h1", "Hack", "qualquer.coisa", "x"));
		assertEquals("Meu perfil", s.texto("pg.perfil.h1"));
		assertFalse(Files.exists(pasta.resolve("personalizacao.properties")));
	}

	@Test
	void corInvalidaERecusada() {
		PersonalizacaoService s = novo();
		assertThrows(IllegalArgumentException.class,
				() -> s.salvarSecao(secao("cores"), Map.of("cor.anunciosFundo", "red;}body{display:none")));
	}

	@Test
	void corDeUmItemValeNoSiteTodo() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("cores"), Map.of("cor.anunciosFundo", "c8102e"));
		assertEquals("#C8102E", s.salvo("cor.anunciosFundo"));
		assertTrue(s.css(Aparencia.padrao()).contains("body .barra-anuncios{background-color:#C8102E !important;}"));
	}

	@Test
	void corDeUmItemNumaPaginaFicaPresaAEssaPaginaEVemDepoisDaGeral() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("pg-login"), Map.of("pg.login.cor.botaoFundo", "#1D4ED8"));
		s.salvarSecao(secao("cores"), Map.of("cor.botaoFundo", "#000000"));
		String css = s.css(Aparencia.padrao());
		assertTrue(css.contains("body.pg-login .login-btn"));
		assertTrue(css.indexOf("body .btn-comprar") < css.indexOf("body.pg-login .btn-comprar"));
	}

	@Test
	void corPrincipalDaPaginaRecalculaOContraste() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("pg-checkout"), Map.of("pg.checkout.cor.principal", "#FACC15"));
		String css = s.css(Aparencia.padrao());
		assertTrue(css.startsWith("body.pg-checkout{--primary-color:#FACC15;--primary-contrast:#0A0A0A;"));
	}

	@Test
	void restaurarSecaoVoltaSoAquelaSecao() throws Exception {
		PersonalizacaoService s = novo();
		s.salvarSecao(secao("pg-login"), Map.of("pg.login.h2", "Oi"));
		s.salvarSecao(secao("pg-perfil"), Map.of("pg.perfil.h1", "Conta"));
		s.restaurarSecao(secao("pg-login"));
		assertEquals("Entrar", s.texto("pg.login.h2"));
		assertEquals("Conta", s.texto("pg.perfil.h1"));
	}

	@Test
	void textoLongoDemaisERecusado() {
		PersonalizacaoService s = novo();
		String grande = "a".repeat(201);
		assertThrows(IllegalArgumentException.class,
				() -> s.salvarSecao(secao("pg-login"), Map.of("pg.login.h2", grande)));
	}

	@Test
	void todaPaginaTemSecaoESeuEscopoDeCss() {
		List<String> ids = CatalogoPersonalizacao.paginas().stream().map(p -> p.id()).toList();
		for (String id : ids) {
			assertEquals("pagina", CatalogoPersonalizacao.secao("pg-" + id).tipo());
		}
		assertEquals("body.pg-login", CatalogoPersonalizacao.pagina("login").escopo());
	}
}
