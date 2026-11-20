package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Pacote;

class EmbalagemFreteTests {

	@Test
	void umaCamisetaSomaPesoDaEmbalagemEUsaMedidaDaPeca() {
		Pacote p = EmbalagemFrete.montar(Map.of(Categoria.CAMISETA, 1));
		assertEquals(new Pacote(280, 30, 22, 3), p);
	}

	@Test
	void pecasDiferentesSomamPesoEEmpilhamAltura() {
		Map<Categoria, Integer> unidades = new EnumMap<>(Categoria.class);
		unidades.put(Categoria.CAMISETA, 2);
		unidades.put(Categoria.MOLETOM, 1);
		Pacote p = EmbalagemFrete.montar(unidades);
		assertEquals(60 + 2 * 220 + 600, p.pesoGramas());
		assertEquals(35, p.comprimentoCm()); // maior comprimento
		assertEquals(28, p.larguraCm()); // maior largura
		assertEquals(2 * 3 + 8, p.alturaCm()); // alturas somadas
	}

	@Test
	void semItensRespeitaOsMinimosDosCorreios() {
		Pacote p = EmbalagemFrete.montar(Map.of());
		assertEquals(new Pacote(60, 16, 11, 2), p);
	}

	@Test
	void alturaTemTeto() {
		Pacote p = EmbalagemFrete.montar(Map.of(Categoria.JAQUETA, 50));
		assertEquals(60, p.alturaCm());
	}

	@Test
	void categoriaNulaContaComoCamiseta() {
		Map<Categoria, Integer> unidades = new java.util.HashMap<>();
		unidades.put(null, 1);
		assertEquals(EmbalagemFrete.montar(Map.of(Categoria.CAMISETA, 1)), EmbalagemFrete.montar(unidades));
	}
}
