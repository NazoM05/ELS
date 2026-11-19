package com.example.nazomocambique.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class Produto {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String nome;

	@Column(length = 50)
	private String cor; // nome da cor (ex.: "Rosa") ou código hexadecimal (ex.: "#e0577c")

	// true = ativo, false = inativo. Produto novo (ex.: tela de cadastro) já nasce ativo.
	@Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean status = true;

	private String tamanho;
	private float preco;
	private String tipo;

	// Classificação da peça (obrigatória nas telas de cadastro e edição). Gravadas pelo nome da constante.
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Categoria categoria;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Genero genero;
	// Nomes dos arquivos de imagem (colunas imagem_frente e imagem_costas). Resolvidos em /img/{nome}:
	// primeiro em uploads/img (enviados pelo admin), depois em static/img (ex.: camisa-militar.svg).
	private String imagemFrente;
	private String imagemCostas;

	// Outras duas posições da galeria (colunas imagem_modelo e imagem_guia_tamanhos), mesma resolução em /img/{nome}:
	// foto da peça vestida em um modelo e imagem com a tabela/guia de medidas.
	@Column(name = "imagem_modelo")
	private String imagemModelo;

	@Column(name = "imagem_guia_tamanhos")
	private String imagemGuiaTamanhos;

	@Column(length = 1000)
	private String descricao;

	// Estoque por tamanho (ex.: P=10, M=3). Só guarda tamanhos que o admin informou a quantidade.
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "produto_estoque", joinColumns = @JoinColumn(name = "produto_id"))
	@MapKeyColumn(name = "tamanho", length = 20)
	@Column(name = "quantidade")
	private Map<String, Integer> estoque = new HashMap<>();

	// Métricas. Ainda não existe módulo de pedidos: "vendidas" fica em 0 até ele existir.
	@Column(columnDefinition = "INTEGER DEFAULT 0")
	private Integer vendidas = 0;

	// Incrementada a cada acesso à página do produto na loja (/detalhes/{id}).
	@Column(columnDefinition = "INTEGER DEFAULT 0")
	private Integer visualizacoes = 0;

	private LocalDateTime criadoEm;
	private LocalDateTime atualizadoEm;

	// Tamanhos oferecidos nas telas de cadastro e edição (letras + numeração)
	public static final List<String> TAMANHOS_PADRAO = List.of("PP", "P", "M", "G", "GG", "XG", "Único",
			"36", "38", "40", "42", "44", "46");

	// Abaixo (ou igual) a esse número o tamanho é sinalizado como "poucas unidades"
	public static final int ESTOQUE_BAIXO = 5;

	private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

	// Cores conhecidas (nome em minúsculas -> hexadecimal usado na amostra da tela de detalhes)
	private static final Map<String, String> CORES_CSS = Map.ofEntries(
			Map.entry("verde militar", "#6e7d47"),
			Map.entry("azul marinho", "#1f3a5f"),
			Map.entry("branco", "#ffffff"),
			Map.entry("rosa claro", "#f4d3dc"),
			Map.entry("preto", "#2b2b2b"),
			Map.entry("grafite", "#3a3a47"),
			Map.entry("cinza", "#8a9095"),
			Map.entry("cinza chumbo", "#797b81"),
			Map.entry("rosa", "#e58ca5"),
			Map.entry("rosa e branco", "#e0577c"),
			Map.entry("azul jeans", "#6e8eb9"),
			Map.entry("azul jeans escuro", "#3a4d67"),
			Map.entry("azul escuro", "#19283f"),
			Map.entry("verde petróleo", "#298c80"),
			Map.entry("caqui", "#8a7a52"),
			Map.entry("lilás", "#b79ad6"),
			Map.entry("pêssego", "#fbe3d6"),
			Map.entry("bege", "#e9d3b4"),
			Map.entry("caramelo", "#b4916d"),
			Map.entry("vermelho", "#b3202a"));

	public Produto() {
	}

	public Produto(Long id, String nome, String cor, boolean status, String tamanho, float preco, String tipo,
			String imagemFrente) {
		super();
		this.id = id;
		this.nome = nome;
		this.cor = cor;
		this.status = status;
		this.tamanho = tamanho;
		this.preco = preco;
		this.tipo = tipo;
		this.imagemFrente = imagemFrente;
	}

	/**
	 * Cor pronta para usar em CSS (#rrggbb) ou null quando a cor está vazia/desconhecida.
	 * Só devolve valores validados, então é seguro usar em th:style.
	 */
	@Transient
	public String getCorCss() {
		if (cor == null) {
			return null;
		}
		String chave = cor.trim().toLowerCase(Locale.ROOT);
		if (chave.matches("#([0-9a-f]{3}|[0-9a-f]{6})")) {
			return chave;
		}
		return CORES_CSS.get(chave);
	}

	/**
	 * Tamanhos disponíveis do produto. O campo "tamanho" guarda os tamanhos separados por vírgula
	 * (ex.: "P,M,G"); este método devolve a lista pronta para a tela.
	 */
	@Transient
	public List<String> getTamanhosDisponiveis() {
		if (tamanho == null || tamanho.isBlank()) {
			return List.of();
		}
		return Arrays.stream(tamanho.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
	}

	/** Resumo para listagens, ex.: "Camiseta · Unissex"; null quando nenhum dos dois foi informado. */
	@Transient
	public String getClassificacao() {
		if (categoria != null && genero != null) {
			return categoria.getRotulo() + " · " + genero.getRotulo();
		}
		if (categoria != null) {
			return categoria.getRotulo();
		}
		return genero != null ? genero.getRotulo() : null;
	}

	/**
	 * Galeria da tela de detalhes, na ordem Frente, Verso, Modelo e Guia de tamanhos.
	 * Só entram as posições com nome de arquivo preenchido (nulo ou em branco é ignorado),
	 * então a tela nunca monta uma miniatura com src vazio.
	 */
	@Transient
	public List<ImagemGaleria> getGaleria() {
		List<ImagemGaleria> itens = new ArrayList<>();
		adicionarNaGaleria(itens, "frente", "Frente", "Frente", imagemFrente);
		adicionarNaGaleria(itens, "verso", "Verso", "Verso", imagemCostas);
		adicionarNaGaleria(itens, "modelo", "Foto com modelo", "Modelo", imagemModelo);
		adicionarNaGaleria(itens, "guia", "Guia de tamanhos (medidas em cm)", "Guia", imagemGuiaTamanhos);
		return itens;
	}

	private void adicionarNaGaleria(List<ImagemGaleria> itens, String chave, String rotulo, String rotuloCurto,
			String arquivo) {
		if (arquivo != null && !arquivo.isBlank()) {
			itens.add(new ImagemGaleria(chave, rotulo, rotuloCurto, arquivo.trim()));
		}
	}

	@PrePersist
	void aoCriar() {
		LocalDateTime agora = LocalDateTime.now();
		this.criadoEm = agora;
		this.atualizadoEm = agora;
	}

	@PreUpdate
	void aoAtualizar() {
		this.atualizadoEm = LocalDateTime.now();
	}

	/**
	 * Tamanhos mostrados como flags nas telas de cadastro/edição: os padrões mais
	 * qualquer tamanho já salvo no produto que não esteja na lista (assim nada se perde).
	 */
	@Transient
	public List<String> getTamanhosOpcoes() {
		Set<String> todos = new LinkedHashSet<>(TAMANHOS_PADRAO);
		todos.addAll(getTamanhosDisponiveis());
		return new ArrayList<>(todos);
	}

	// ---------- Insights da tela "Visualizar produto" ----------

	/** Soma do estoque informado; null quando nenhum tamanho tem estoque informado. */
	@Transient
	public Integer getEstoqueTotal() {
		if (estoque == null || estoque.isEmpty()) {
			return null;
		}
		return estoque.values().stream().filter(q -> q != null).mapToInt(Integer::intValue).sum();
	}

	/** Uma linha por tamanho disponível, com a situação do estoque (OK / BAIXO / ESGOTADO / NAO_INFORMADO). */
	@Transient
	public List<EstoqueTamanho> getEstoquePorTamanho() {
		List<EstoqueTamanho> linhas = new ArrayList<>();
		for (String t : getTamanhosDisponiveis()) {
			Integer q = estoque == null ? null : estoque.get(t);
			String situacao = q == null ? "NAO_INFORMADO" : q <= 0 ? "ESGOTADO" : q <= ESTOQUE_BAIXO ? "BAIXO" : "OK";
			linhas.add(new EstoqueTamanho(t, q, situacao));
		}
		return linhas;
	}

	@Transient
	public boolean isTemAlertaEstoque() {
		return getEstoquePorTamanho().stream()
				.anyMatch(l -> l.getSituacao().equals("ESGOTADO") || l.getSituacao().equals("BAIXO"));
	}

	@Transient
	public int getVendidasTotal() {
		return vendidas == null ? 0 : vendidas;
	}

	@Transient
	public int getVisualizacoesTotal() {
		return visualizacoes == null ? 0 : visualizacoes;
	}

	/** Receita estimada = unidades vendidas x preço atual (não há histórico de pedidos). */
	@Transient
	public BigDecimal getReceita() {
		return BigDecimal.valueOf(preco).multiply(BigDecimal.valueOf(getVendidasTotal()));
	}

	/** Conversão em % (vendidas / visualizações); null quando ainda não há visualizações. */
	@Transient
	public Double getTaxaConversao() {
		int views = getVisualizacoesTotal();
		return views == 0 ? null : getVendidasTotal() * 100.0 / views;
	}

	@Transient
	public String getCriadoEmFormatado() {
		return criadoEm == null ? null : criadoEm.format(FORMATO_DATA);
	}

	@Transient
	public String getAtualizadoEmFormatado() {
		return atualizadoEm == null ? null : atualizadoEm.format(FORMATO_DATA);
	}

	/** Uma foto da galeria da tela de detalhes (frente, verso, modelo ou guia de tamanhos). */
	public static class ImagemGaleria {
		private final String chave;
		private final String rotulo;
		private final String rotuloCurto;
		private final String arquivo;

		public ImagemGaleria(String chave, String rotulo, String rotuloCurto, String arquivo) {
			this.chave = chave;
			this.rotulo = rotulo;
			this.rotuloCurto = rotuloCurto;
			this.arquivo = arquivo;
		}

		public String getChave() { return chave; }
		public String getRotulo() { return rotulo; }
		public String getRotuloCurto() { return rotuloCurto; }
		public String getArquivo() { return arquivo; }
	}

	/** Linha da tabela de estoque por tamanho. */
	public static class EstoqueTamanho {
		private final String tamanho;
		private final Integer quantidade;
		private final String situacao;

		public EstoqueTamanho(String tamanho, Integer quantidade, String situacao) {
			this.tamanho = tamanho;
			this.quantidade = quantidade;
			this.situacao = situacao;
		}

		public String getTamanho() { return tamanho; }
		public Integer getQuantidade() { return quantidade; }
		public String getSituacao() { return situacao; }
	}
}
