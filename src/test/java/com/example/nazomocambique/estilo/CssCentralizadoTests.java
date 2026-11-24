package com.example.nazomocambique.estilo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Garante a regra do AGENTS.md: todo o CSS fica em static/css/site.css e cada
 * página o recebe pelo fragmento fragments/css.html.
 */
class CssCentralizadoTests {

	private static final Path TEMPLATES = Path.of("src/main/resources/templates");
	private static final Path STATIC = Path.of("src/main/resources/static");
	private static final Path FRAGMENTO_CSS = TEMPLATES.resolve("fragments/css.html");
	private static final Pattern STYLE_INLINE = Pattern.compile("(^|[\\s<`])style=\"");

	private static List<Path> arquivos(Path raiz, String extensao) throws IOException {
		try (Stream<Path> s = Files.walk(raiz)) {
			return s.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(extensao)).toList();
		}
	}

	private static String ler(Path p) throws IOException {
		return Files.readString(p, StandardCharsets.UTF_8);
	}

	@Test
	void existeApenasUmArquivoCssNoProjeto() throws IOException {
		List<Path> css = arquivos(STATIC, ".css");
		assertEquals(List.of(STATIC.resolve("css/site.css")), css, "Só pode existir static/css/site.css");
	}

	@Test
	void nenhumTemplateTemBlocoStyleForaDoFragmentoCss() throws IOException {
		for (Path p : arquivos(TEMPLATES, ".html")) {
			if (p.equals(FRAGMENTO_CSS)) continue;
			assertTrue(!ler(p).contains("<style"), "Bloco <style> proibido em " + p + " (use site.css)");
		}
	}

	@Test
	void nenhumTemplateOuScriptUsaStyleInline() throws IOException {
		List<Path> alvos = Stream.concat(arquivos(TEMPLATES, ".html").stream(), arquivos(STATIC, ".js").stream()).toList();
		for (Path p : alvos) {
			assertTrue(!STYLE_INLINE.matcher(ler(p)).find(), "style=\"...\" proibido em " + p + " (crie uma classe em site.css)");
		}
	}

	@Test
	void todaPaginaChamaOFragmentoCssEMaisNenhumaFolhaDeEstilo() throws IOException {
		for (Path p : arquivos(TEMPLATES, ".html")) {
			if (p.startsWith(TEMPLATES.resolve("fragments"))) continue;
			String html = ler(p);
			assertTrue(html.contains("~{fragments/css :: css}"), "Página sem o fragmento de CSS: " + p);
			assertTrue(!html.contains("rel=\"stylesheet\""), "Folha de estilo direta proibida em " + p);
		}
	}

	@Test
	void fragmentoCarregaOSiteCss() throws IOException {
		assertTrue(ler(FRAGMENTO_CSS).contains("@{/css/site.css("), "fragments/css.html deve carregar /css/site.css");
	}
}
