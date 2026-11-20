package com.example.nazomocambique.service;

import java.util.EnumMap;
import java.util.Map;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Pacote;

/**
 * Estima peso e medidas do pacote a partir do que está no carrinho. O cadastro de produto ainda não
 * tem peso nem dimensões, então cada categoria tem um tamanho médio (peça dobrada, em gramas e cm).
 * AJUSTE os números abaixo com as medidas reais das suas peças: o preço dos Correios depende deles.
 */
public final class EmbalagemFrete {

	/** peso (g), comprimento, largura e altura (cm) de UMA peça já embalada. */
	private record Medida(int peso, int comprimento, int largura, int altura) {
	}

	private static final Map<Categoria, Medida> MEDIDAS = new EnumMap<>(Categoria.class);

	static {
		MEDIDAS.put(Categoria.CAMISETA, new Medida(220, 30, 22, 3));
		MEDIDAS.put(Categoria.CAMISA, new Medida(280, 32, 24, 4));
		MEDIDAS.put(Categoria.BLUSA, new Medida(250, 30, 22, 4));
		MEDIDAS.put(Categoria.MOLETOM, new Medida(600, 35, 28, 8));
		MEDIDAS.put(Categoria.JAQUETA, new Medida(750, 38, 30, 10));
		MEDIDAS.put(Categoria.CALCA, new Medida(550, 35, 28, 6));
		MEDIDAS.put(Categoria.BERMUDA, new Medida(350, 30, 25, 5));
		MEDIDAS.put(Categoria.SAIA, new Medida(250, 30, 22, 4));
		MEDIDAS.put(Categoria.VESTIDO, new Medida(350, 32, 24, 5));
		MEDIDAS.put(Categoria.ACESSORIOS, new Medida(100, 20, 15, 3));
	}

	/** Peso da caixa/saco de envio, somado uma vez por pedido. */
	private static final int PESO_EMBALAGEM = 60;
	/** Mínimos aceitos pelos Correios para pacote (cm). */
	private static final int MIN_COMPRIMENTO = 16, MIN_LARGURA = 11, MIN_ALTURA = 2;
	/** Teto de altura da pilha (cm), para não gerar uma medida absurda. */
	private static final int MAX_ALTURA = 60;

	private EmbalagemFrete() {
	}

	/**
	 * @param unidadesPorCategoria quantas peças de cada categoria vão no pedido (categoria nula conta como camiseta)
	 */
	public static Pacote montar(Map<Categoria, Integer> unidadesPorCategoria) {
		int peso = PESO_EMBALAGEM;
		int comprimento = 0;
		int largura = 0;
		int altura = 0;
		for (Map.Entry<Categoria, Integer> e : unidadesPorCategoria.entrySet()) {
			int qtd = e.getValue() == null ? 0 : e.getValue();
			if (qtd <= 0) {
				continue;
			}
			Medida m = MEDIDAS.getOrDefault(e.getKey() == null ? Categoria.CAMISETA : e.getKey(),
					MEDIDAS.get(Categoria.CAMISETA));
			peso += m.peso() * qtd;
			comprimento = Math.max(comprimento, m.comprimento());
			largura = Math.max(largura, m.largura());
			altura += m.altura() * qtd; // as peças ficam empilhadas
		}
		return new Pacote(peso,
				Math.max(comprimento, MIN_COMPRIMENTO),
				Math.max(largura, MIN_LARGURA),
				Math.min(Math.max(altura, MIN_ALTURA), MAX_ALTURA));
	}
}
