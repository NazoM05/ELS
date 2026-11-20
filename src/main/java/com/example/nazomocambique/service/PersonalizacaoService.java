package com.example.nazomocambique.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.nazomocambique.model.Aparencia;
import com.example.nazomocambique.model.CatalogoPersonalizacao;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Campo;
import com.example.nazomocambique.model.CatalogoPersonalizacao.ItemCor;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Pagina;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Secao;

import jakarta.annotation.PostConstruct;

/**
 * Textos e cores personalizados na tela Configurações (ver CatalogoPersonalizacao).
 *
 * Só guarda o que difere do padrão, em uploads/config/personalizacao.properties (pelo mesmo motivo da
 * AparenciaService: o H2 do projeto é em memória). O estado atual fica em memória e é trocado inteiro a
 * cada gravação, então as telas leem sem tocar o disco nem precisar de trava.
 */
@Service
public class PersonalizacaoService {

	private static final Pattern COR_HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");
	// Caracteres de controle (menos a quebra de linha, permitida nos textos longos)
	private static final Pattern CONTROLE = Pattern.compile("[\\p{Cntrl}&&[^\\n]]");

	private final Path arquivo;

	private volatile Map<String, String> salvos = Map.of();
	private volatile Map<String, String> textos = montarTextos(Map.of());

	@Autowired
	public PersonalizacaoService() {
		this(Paths.get("uploads/config/personalizacao.properties"));
	}

	PersonalizacaoService(Path arquivo) {
		this.arquivo = arquivo;
	}

	@PostConstruct
	void carregar() {
		if (!Files.isRegularFile(arquivo)) {
			return;
		}
		Properties p = new Properties();
		try (Reader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
			p.load(leitor);
		} catch (IOException e) {
			e.printStackTrace();
			return; // arquivo ilegível: segue com o padrão
		}
		Map<String, String> lidos = new TreeMap<>();
		for (String chave : p.stringPropertyNames()) {
			String valor = p.getProperty(chave);
			// Ignora o que não é mais do catálogo ou não passa na validação (arquivo editado à mão)
			try {
				String normalizado = normalizar(chave, valor);
				if (normalizado != null) {
					lidos.put(chave, normalizado);
				}
			} catch (IllegalArgumentException e) {
				// valor inválido: fica o padrão
			}
		}
		aplicar(lidos);
	}

	// ---------- leitura ----------

	/** Todos os textos do catálogo já com o valor em uso (personalizado ou padrão). */
	public Map<String, String> textos() {
		return textos;
	}

	public String texto(String chave) {
		return textos.getOrDefault(chave, "");
	}

	/** Valor salvo para a chave, ou null se está no padrão. */
	public String salvo(String chave) {
		return salvos.get(chave);
	}

	/** Avisos da barra do topo que não foram deixados em branco. */
	public List<String> anuncios() {
		List<String> lista = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			String a = texto("geral.anuncio" + i);
			if (!a.isBlank()) {
				lista.add(a);
			}
		}
		return lista;
	}

	/** Quantos itens a seção tem fora do padrão (mostrado no menu da tela de configurações). */
	public int alteradosNaSecao(Secao secao) {
		int n = 0;
		for (String chave : CatalogoPersonalizacao.chavesDaSecao(secao)) {
			if (salvos.containsKey(chave)) {
				n++;
			}
		}
		return n;
	}

	/**
	 * CSS das cores personalizadas: primeiro as do site todo, depois as de cada página (seletor mais
	 * específico, então vencem). Só tem seletores fixos do catálogo e cores validadas.
	 */
	public String css(Aparencia aparencia) {
		boolean logo = aparencia != null && aparencia.isLogoPersonalizada();
		StringBuilder geral = new StringBuilder();
		StringBuilder paginas = new StringBuilder();
		for (Map.Entry<String, String> e : salvos.entrySet()) {
			ItemCor item = CatalogoPersonalizacao.itemDaChave(e.getKey());
			if (item == null) {
				continue;
			}
			Pagina pagina = CatalogoPersonalizacao.paginaDaChave(e.getKey());
			String cor = e.getValue();
			String regra = regra(item, pagina == null ? CatalogoPersonalizacao.ESCOPO_GERAL : pagina.escopo(), cor,
					Aparencia.corContraste(cor), Aparencia.filtroLogo(cor, logo));
			(pagina == null ? geral : paginas).append(regra);
		}
		return geral.append(paginas).toString();
	}

	/**
	 * Regra de CSS de um item num escopo ("body" ou "body.pg-x"). A tela de configurações também usa
	 * com marcadores no lugar da cor, para montar a prévia ao vivo.
	 */
	public static String regra(ItemCor item, String escopo, String cor, String contraste, String filtro) {
		if (item.isVariavel()) {
			if ("--primary-color".equals(item.variavel())) {
				// Os tons derivados são calculados onde são declarados: precisam ser repetidos no escopo
				return escopo + "{--primary-color:" + cor + ";--primary-contrast:" + contraste + ";--logo-filtro:" + filtro
						+ ";--primary-soft:color-mix(in srgb,var(--primary-contrast) 62%,var(--primary-color))"
						+ ";--primary-faint:color-mix(in srgb,var(--primary-contrast) 42%,var(--primary-color))"
						+ ";--primary-line:color-mix(in srgb,var(--primary-contrast) 12%,var(--primary-color))"
						+ ";--primary-line-forte:color-mix(in srgb,var(--primary-contrast) 17%,var(--primary-color))"
						+ ";--primary-hover:color-mix(in srgb,var(--primary-contrast) 10%,var(--primary-color));}";
			}
			return escopo + "{" + item.variavel() + ":" + cor + ";}";
		}
		List<String> seletores = new ArrayList<>();
		List<String> hovers = new ArrayList<>();
		for (String parte : item.seletor().split(",")) {
			String s = escopo + " " + parte.trim();
			seletores.add(s);
			hovers.add(s + ":hover");
		}
		StringBuilder css = new StringBuilder(String.join(",", seletores)).append('{');
		for (String prop : item.propriedades()) {
			css.append(prop).append(':').append(cor).append(" !important;");
		}
		css.append('}');
		if (item.hover()) {
			// A cor fixa anula o hover original; um leve escurecimento mantém o retorno visual
			css.append(String.join(",", hovers)).append("{filter:brightness(.9);}");
		}
		return css.toString();
	}

	// ---------- gravação ----------

	/**
	 * Grava os valores enviados pelo formulário de uma seção. Chaves fora da seção são ignoradas;
	 * valor igual ao padrão (ou vazio, quando o campo não pode ficar vazio) volta ao padrão.
	 * @throws IllegalArgumentException com mensagem pronta para mostrar ao admin
	 */
	public synchronized void salvarSecao(Secao secao, Map<String, String> enviados) throws IOException {
		Map<String, String> novos = new TreeMap<>(salvos);
		for (String chave : CatalogoPersonalizacao.chavesDaSecao(secao)) {
			if (!enviados.containsKey(chave)) {
				continue; // campo não veio no formulário: mantém
			}
			String valor = normalizar(chave, enviados.get(chave));
			if (valor == null) {
				novos.remove(chave);
			} else {
				novos.put(chave, valor);
			}
		}
		persistir(novos);
		aplicar(novos);
	}

	/** Volta todos os itens da seção ao padrão. */
	public synchronized void restaurarSecao(Secao secao) throws IOException {
		Map<String, String> novos = new TreeMap<>(salvos);
		CatalogoPersonalizacao.chavesDaSecao(secao).forEach(novos::remove);
		persistir(novos);
		aplicar(novos);
	}

	/**
	 * Valor pronto para guardar, ou null quando o certo é voltar ao padrão.
	 * @throws IllegalArgumentException cor inválida ou texto longo demais
	 */
	String normalizar(String chave, String valor) {
		ItemCor item = CatalogoPersonalizacao.itemDaChave(chave);
		if (item != null) {
			if (valor == null || valor.isBlank()) {
				return null;
			}
			String cor = valor.trim();
			if (!cor.startsWith("#")) {
				cor = "#" + cor;
			}
			if (!COR_HEX.matcher(cor).matches()) {
				throw new IllegalArgumentException("Cor inválida em \"" + item.rotulo() + "\". Use o formato #RRGGBB, por exemplo #C8102E.");
			}
			return cor.toUpperCase();
		}
		Campo campo = CatalogoPersonalizacao.campo(chave);
		if (campo == null) {
			return null; // não é do catálogo: nunca é gravada
		}
		String texto = valor == null ? "" : valor.replace("\r\n", "\n").replace('\r', '\n');
		if (!campo.longo()) {
			texto = texto.replace('\n', ' ');
		}
		texto = CONTROLE.matcher(texto).replaceAll("").strip();
		if (texto.length() > campo.limite()) {
			throw new IllegalArgumentException("\"" + campo.rotulo() + "\" pode ter no máximo " + campo.limite() + " caracteres.");
		}
		if (texto.equals(campo.padrao()) || (texto.isEmpty() && !campo.opcional())) {
			return null;
		}
		return texto;
	}

	private void persistir(Map<String, String> valores) throws IOException {
		if (valores.isEmpty()) {
			Files.deleteIfExists(arquivo);
			return;
		}
		Properties p = new Properties();
		p.putAll(valores);
		if (arquivo.getParent() != null) {
			Files.createDirectories(arquivo.getParent());
		}
		try (Writer escritor = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
			p.store(escritor, "Textos e cores do site (gerado pela tela Configuracoes)");
		}
	}

	private void aplicar(Map<String, String> valores) {
		Map<String, String> copia = Collections.unmodifiableMap(new TreeMap<>(valores));
		textos = montarTextos(copia);
		salvos = copia;
	}

	private static Map<String, String> montarTextos(Map<String, String> valores) {
		Map<String, String> mapa = new LinkedHashMap<>();
		CatalogoPersonalizacao.campos().forEach((chave, campo) -> mapa.put(chave, valores.getOrDefault(chave, campo.padrao())));
		return Collections.unmodifiableMap(mapa);
	}
}
