package com.example.nazomocambique.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dados de paginação prontos para a tela (usados pelo fragmento "paginacao").
 * Números de página são 1-based (é o que aparece na URL: ?pagina=2).
 * Os links gerados mantêm os filtros ativos (busca, categoria, gênero); a página 1 sai sem "pagina".
 */
public class Paginacao {

	// Quantas páginas aparecem de cada lado da atual antes de virar "…"
	private static final int VIZINHAS = 2;

	private final int paginaAtual;
	private final int totalPaginas;
	private final long totalItens;
	private final String caminho;
	private final Map<String, String> filtros = new LinkedHashMap<>();

	public Paginacao(int paginaAtual, int totalPaginas, long totalItens, String caminho, Map<String, String> filtros) {
		this.totalPaginas = Math.max(totalPaginas, 1);
		this.paginaAtual = Math.min(Math.max(paginaAtual, 1), this.totalPaginas);
		this.totalItens = totalItens;
		this.caminho = caminho;
		if (filtros != null) {
			filtros.forEach((nome, valor) -> {
				if (valor != null && !valor.isBlank()) {
					this.filtros.put(nome, valor);
				}
			});
		}
	}

	public int getPaginaAtual() { return paginaAtual; }
	public int getTotalPaginas() { return totalPaginas; }
	public long getTotalItens() { return totalItens; }

	/** Controles só aparecem quando há mais de uma página. */
	public boolean isTemVariasPaginas() { return totalPaginas > 1; }
	public boolean isTemAnterior() { return paginaAtual > 1; }
	public boolean isTemProxima() { return paginaAtual < totalPaginas; }

	public String getUrlAnterior() { return url(paginaAtual - 1); }
	public String getUrlProxima() { return url(paginaAtual + 1); }

	/** Endereço (sem context-path) de uma página, com os filtros preservados. Página inválida é ajustada ao limite. */
	public String url(int pagina) {
		int alvo = Math.min(Math.max(pagina, 1), totalPaginas);
		StringBuilder sb = new StringBuilder(caminho);
		char separador = '?';
		for (Map.Entry<String, String> f : filtros.entrySet()) {
			sb.append(separador).append(f.getKey()).append('=')
					.append(URLEncoder.encode(f.getValue(), StandardCharsets.UTF_8));
			separador = '&';
		}
		if (alvo > 1) {
			sb.append(separador).append("pagina=").append(alvo);
		}
		return sb.toString();
	}

	/** Botões numéricos: primeira, última e a janela ao redor da atual, com "…" nos buracos. */
	public List<Item> getItens() {
		List<Item> itens = new ArrayList<>();
		int anterior = 0;
		for (int n = 1; n <= totalPaginas; n++) {
			boolean mostrar = n == 1 || n == totalPaginas || Math.abs(n - paginaAtual) <= VIZINHAS;
			if (!mostrar) {
				continue;
			}
			if (n - anterior == 2) {
				// Buraco de uma só página: mostra o número em vez de "…"
				itens.add(new Item(n - 1, false, false, url(n - 1)));
			} else if (n - anterior > 2) {
				itens.add(new Item(0, true, false, null));
			}
			itens.add(new Item(n, false, n == paginaAtual, url(n)));
			anterior = n;
		}
		return itens;
	}

	/** Um botão da barra: número, reticências ("…") ou a página atual. */
	public static class Item {
		private final int numero;
		private final boolean reticencias;
		private final boolean atual;
		private final String url;

		Item(int numero, boolean reticencias, boolean atual, String url) {
			this.numero = numero;
			this.reticencias = reticencias;
			this.atual = atual;
			this.url = url;
		}

		public int getNumero() { return numero; }
		public boolean isReticencias() { return reticencias; }
		public boolean isAtual() { return atual; }
		public String getUrl() { return url; }
	}
}
