package com.example.nazomocambique.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.nazomocambique.model.Aparencia;

import jakarta.annotation.PostConstruct;

/**
 * Guarda a identidade visual do site (cor principal e logo).
 *
 * As preferências ficam em arquivo (uploads/config/aparencia.properties) e a logo em uploads/logo,
 * e não no H2: o banco do projeto é em memória (jdbc:h2:mem) e voltaria ao padrão a cada reinício.
 * O valor atual fica em memória, então ler a aparência a cada requisição não toca o disco.
 */
@Service
public class AparenciaService {

	// Servida em /logo-site/** pelo ImagensWebConfig
	public static final Path PASTA_LOGO = Paths.get("uploads/logo");
	private static final Path ARQUIVO_CONFIG = Paths.get("uploads/config/aparencia.properties");

	private static final long TAMANHO_MAXIMO_LOGO = 2L * 1024 * 1024; // 2 MB
	private static final Pattern COR_HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");
	private static final Pattern NOME_LOGO = Pattern.compile("^[0-9a-f\\-]{36}\\.(png|jpg|webp|gif)$");

	private volatile Aparencia atual = Aparencia.padrao();

	@PostConstruct
	void carregar() {
		if (!Files.isRegularFile(ARQUIVO_CONFIG)) {
			return;
		}
		Properties p = new Properties();
		try (Reader leitor = Files.newBufferedReader(ARQUIVO_CONFIG, StandardCharsets.UTF_8)) {
			p.load(leitor);
		} catch (IOException e) {
			e.printStackTrace();
			return; // arquivo ilegível: segue com o padrão
		}
		String cor = p.getProperty("corPrimaria", Aparencia.COR_PADRAO);
		if (!COR_HEX.matcher(cor).matches()) {
			cor = Aparencia.COR_PADRAO;
		}
		String logo = p.getProperty("logoArquivo");
		if (logo == null || !NOME_LOGO.matcher(logo).matches() || !Files.isRegularFile(PASTA_LOGO.resolve(logo))) {
			logo = null; // nome inválido ou arquivo sumiu: volta à logo padrão
		}
		atual = new Aparencia(cor.toUpperCase(), logo);
	}

	public Aparencia obter() {
		return atual;
	}

	/**
	 * Salva a cor e, se veio um arquivo, troca a logo. Sem arquivo, a logo atual é mantida.
	 * @throws IllegalArgumentException com mensagem pronta para mostrar ao admin
	 */
	public synchronized void salvar(String cor, MultipartFile logo) throws IOException {
		if (cor == null || !COR_HEX.matcher(cor.trim()).matches()) {
			throw new IllegalArgumentException("Cor inválida. Use o formato #RRGGBB, por exemplo #C8102E.");
		}
		String corNormalizada = cor.trim().toUpperCase();

		String logoAntiga = atual.getLogoArquivo();
		String logoNova = logoAntiga;
		boolean trocouLogo = logo != null && !logo.isEmpty();
		if (trocouLogo) {
			logoNova = gravarLogo(logo);
		}
		try {
			persistir(corNormalizada, logoNova);
		} catch (IOException | RuntimeException e) {
			if (trocouLogo) {
				apagarLogo(logoNova); // não deixa arquivo órfão se não conseguiu gravar a configuração
			}
			throw e;
		}
		atual = new Aparencia(corNormalizada, logoNova);
		if (trocouLogo) {
			apagarLogo(logoAntiga);
		}
	}

	/** Volta à cor e à logo originais do projeto. */
	public synchronized void restaurarPadrao() throws IOException {
		String logoAntiga = atual.getLogoArquivo();
		Files.deleteIfExists(ARQUIVO_CONFIG);
		atual = Aparencia.padrao();
		apagarLogo(logoAntiga);
	}

	private void persistir(String cor, String logo) throws IOException {
		Properties p = new Properties();
		p.setProperty("corPrimaria", cor);
		if (logo != null) {
			p.setProperty("logoArquivo", logo);
		}
		Files.createDirectories(ARQUIVO_CONFIG.getParent());
		try (Writer escritor = Files.newBufferedWriter(ARQUIVO_CONFIG, StandardCharsets.UTF_8)) {
			p.store(escritor, "Aparencia do site (gerado pela tela Configuracoes)");
		}
	}

	// Valida pelo CONTEÚDO do arquivo (não pelo nome nem pelo Content-Type, que o cliente controla)
	private String gravarLogo(MultipartFile arquivo) throws IOException {
		if (arquivo.getSize() > TAMANHO_MAXIMO_LOGO) {
			throw new IllegalArgumentException("A logo deve ter no máximo 2 MB.");
		}
		byte[] bytes = arquivo.getBytes();
		String extensao = detectarExtensao(bytes);
		if (extensao == null) {
			throw new IllegalArgumentException("Formato não aceito. Envie a logo em PNG, JPG, WEBP ou GIF.");
		}
		String nome = UUID.randomUUID() + "." + extensao;
		Files.createDirectories(PASTA_LOGO);
		Files.write(PASTA_LOGO.resolve(nome), bytes);
		return nome;
	}

	private static String detectarExtensao(byte[] b) {
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
			return "png";
		}
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return "jpg";
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
				&& b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
			return "webp";
		}
		if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
			return "gif";
		}
		return null; // SVG fica de fora de propósito: pode carregar script
	}

	private void apagarLogo(String nome) {
		if (nome == null || !NOME_LOGO.matcher(nome).matches()) {
			return; // só apaga arquivos com o nome que o próprio sistema gera
		}
		try {
			Files.deleteIfExists(PASTA_LOGO.resolve(nome));
		} catch (IOException e) {
			e.printStackTrace(); // sobra de arquivo não deve derrubar a operação
		}
	}
}
