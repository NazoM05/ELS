package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import com.example.nazomocambique.model.Ordenacao;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.repository.ProdutoRepository;

/**
 * Produtos de coleção especial (Vice City) só aparecem na página da coleção:
 * o catálogo normal (Início e Outlet) consulta com colecao nula, e a página da coleção pede VICE_CITY.
 */
class ColecaoCatalogoTests {

	// Posições dos argumentos de ProdutoRepository.filtrar(termo, categoria, g1, g2, outlet, colecao, pageable)
	private static final int ARG_OUTLET = 4;
	private static final int ARG_COLECAO = 5;

	/** Repositório falso que só guarda os argumentos recebidos por filtrar(...). */
	private static final class Captura {
		Object[] args;

		ProdutoRepository repositorio() {
			return (ProdutoRepository) Proxy.newProxyInstance(ColecaoCatalogoTests.class.getClassLoader(),
					new Class<?>[] { ProdutoRepository.class }, (proxy, metodo, argumentos) -> {
						if (metodo.getName().equals("filtrar")) {
							args = argumentos;
							return new PageImpl<Produto>(List.of());
						}
						throw new UnsupportedOperationException(metodo.getName());
					});
		}
	}

	@Test
	void catalogoNormalNaoIncluiProdutosDeColecao() {
		Captura captura = new Captura();

		new ProdutoService(captura.repositorio()).filtrar("", null, null, 0);

		assertEquals(false, captura.args[ARG_OUTLET]);
		assertNull(captura.args[ARG_COLECAO]);
	}

	@Test
	void outletNaoIncluiProdutosDeColecao() {
		Captura captura = new Captura();

		new ProdutoService(captura.repositorio()).filtrar("", null, null, Ordenacao.PADRAO, 0, true);

		assertEquals(true, captura.args[ARG_OUTLET]);
		assertNull(captura.args[ARG_COLECAO]);
	}

	@Test
	void paginaDaColecaoPedeSomenteViceCity() {
		Captura captura = new Captura();

		new ProdutoService(captura.repositorio()).filtrar("", null, null, Ordenacao.PADRAO, 0, false,
				Produto.COLECAO_VICE_CITY);

		assertEquals("VICE_CITY", captura.args[ARG_COLECAO]);
	}
}
