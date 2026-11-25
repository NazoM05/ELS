package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.repository.ProdutoRepository;

/** Produto inativo não aparece na vitrine (home, busca, outlet, coleções e recomendados). */
// Banco próprio: o data.sql roda de novo neste contexto
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:catalogo-inativos")
class CatalogoProdutosInativosTests {

	@Autowired
	private ProdutoService produtoService;

	@Autowired
	private ProdutoRepository produtoRepository;

	private Produto novo(String nome, boolean ativo) {
		Produto p = new Produto();
		p.setNome(nome);
		p.setPreco(40.0f);
		p.setTamanho("P,M");
		p.setCategoria(Categoria.CAMISETA);
		p.setGenero(Genero.UNISSEX);
		p.setStatus(ativo);
		Map<String, Integer> estoque = new LinkedHashMap<>();
		estoque.put("P", 3);
		estoque.put("M", 3);
		p.setEstoque(estoque);
		return produtoRepository.save(p);
	}

	@Test
	void vitrine_mostra_o_ativo_e_esconde_o_inativo() {
		String sufixo = String.valueOf(System.nanoTime());
		novo("Zzativo " + sufixo, true);
		novo("Zzinativo " + sufixo, false);

		var pagina = produtoService.filtrar("Zz", null, null, 0);

		assertTrue(pagina.getContent().stream().anyMatch(p -> p.getNome().equals("Zzativo " + sufixo)));
		assertTrue(pagina.getContent().stream().noneMatch(p -> p.getNome().equals("Zzinativo " + sufixo)));
		assertEquals(1, pagina.getTotalElements());
	}

	@Test
	void inativo_continua_na_listagem_do_admin() {
		String nome = "Zzadmin " + System.nanoTime();
		novo(nome, false);

		assertTrue(produtoService.listarTodos().stream().anyMatch(p -> p.getNome().equals(nome)));
	}
}
