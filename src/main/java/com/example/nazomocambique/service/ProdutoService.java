
package com.example.nazomocambique.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.Ordenacao;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.repository.ProdutoRepository;

@Service
public class ProdutoService {

    /** Máximo de produtos por página na vitrine. */
    public static final int PRODUTOS_POR_PAGINA = 20;
   
    private ProdutoRepository produtoRepository;
    
    public ProdutoService(ProdutoRepository produtoRepository) {
    	this.produtoRepository = produtoRepository;
    }
    
    

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }
    
    public Produto buscarPorId(Long id) throws Exception {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new Exception("Produto com ID " + id + " não encontrado."));
    }

    public Produto salvarProduto(Produto produto) {
        if (produto.getPreco() < 0) {
            throw new IllegalArgumentException("O preço do produto não pode ser negativo");
        }
        return produtoRepository.save(produto);
    }



	public List<Produto> pesquisarPorNome(String termoPesquisa) {
		if(termoPesquisa.isBlank()) {
			return listarTodos();
		} else {
			return produtoRepository.findAllByNomeContainingIgnoreCase(termoPesquisa.trim());
		}
	}

	/**
	 * Catálogo da loja: termo (parte do nome), categoria e gênero, todos opcionais e combináveis.
	 * Filtrar por Masculino ou Feminino também traz os produtos Unissex.
	 * Devolve uma página de até {@value #PRODUTOS_POR_PAGINA} produtos. O índice é 0-based e é ajustado
	 * quando inválido: negativo vira a primeira página e além do fim vira a última.
	 */
	public Page<Produto> filtrar(String termo, Categoria categoria, Genero genero, int pagina) {
		return filtrar(termo, categoria, genero, Ordenacao.PADRAO, pagina);
	}

	/** Igual ao anterior, com a ordem da vitrine (padrão, novidades ou menor preço). */
	public Page<Produto> filtrar(String termo, Categoria categoria, Genero genero, Ordenacao ordenacao, int pagina) {
		return filtrar(termo, categoria, genero, ordenacao, pagina, false);
	}

	/** Catálogo segmentado: por padrão exclui Outlet; true retorna somente produtos Outlet. Nunca inclui coleções especiais. */
	public Page<Produto> filtrar(String termo, Categoria categoria, Genero genero, Ordenacao ordenacao, int pagina, boolean outlet) {
		return filtrar(termo, categoria, genero, ordenacao, pagina, outlet, null);
	}

	/**
	 * Catálogo de uma coleção especial (ex.: {@link Produto#COLECAO_VICE_CITY}): mesmos filtros da vitrine,
	 * mas só com produtos da coleção. Com colecao nulo é o catálogo normal, sem produtos de coleção.
	 */
	public Page<Produto> filtrar(String termo, Categoria categoria, Genero genero, Ordenacao ordenacao, int pagina,
			boolean outlet, String colecao) {
		Ordenacao ordem = ordenacao == null ? Ordenacao.PADRAO : ordenacao;
		String termoLimpo = (termo == null || termo.isBlank()) ? null : termo.trim();
		Genero g1 = genero;
		Genero g2 = genero == null ? null : Genero.UNISSEX;
		int indice = Math.max(pagina, 0);
		Page<Produto> resultado = produtoRepository.filtrar(termoLimpo, categoria, g1, g2,
				outlet, colecao, PageRequest.of(indice, PRODUTOS_POR_PAGINA, ordem.getSort()));
		if (resultado.getTotalPages() > 0 && indice >= resultado.getTotalPages()) {
			resultado = produtoRepository.filtrar(termoLimpo, categoria, g1, g2,
					outlet, colecao, PageRequest.of(resultado.getTotalPages() - 1, PRODUTOS_POR_PAGINA, ordem.getSort()));
		}
		return resultado;
	}

	/**
	 * Salva o que veio dos formulários de cadastro e edição.
	 * Na edição, carrega o produto do banco e atualiza só os campos do formulário, para não
	 * perder dados que a tela não envia (vendidas, visualizações, data de criação).
	 */
	@Transactional
	public Produto salvarFormulario(Produto form) throws Exception {
		if (form.getCategoria() == null || form.getGenero() == null) {
			throw new IllegalArgumentException("Informe a categoria e o gênero do produto.");
		}
		if (form.isOutlet()) {
			if (form.getDescontoOutlet() == null || form.getDescontoOutlet() < 1 || form.getDescontoOutlet() > 100) {
				throw new IllegalArgumentException("O desconto do Outlet deve estar entre 1% e 100%.");
			}
		} else {
			form.setDescontoOutlet(null);
		}
		Map<String, Integer> estoque = estoqueDosTamanhosMarcados(form);
		if (form.getId() == null) {
			form.setEstoque(estoque);
			return salvarProduto(form);
		}
		Produto atual = buscarPorId(form.getId());
		atual.setNome(form.getNome());
		atual.setPreco(form.getPreco());
		atual.setCor(form.getCor());
		atual.setTipo(form.getTipo());
		atual.setCategoria(form.getCategoria());
		atual.setGenero(form.getGenero());
		atual.setTamanho(form.getTamanho());
		atual.setStatus(form.isStatus());
		atual.setOutlet(form.isOutlet());
		atual.setDescontoOutlet(form.isOutlet() ? form.getDescontoOutlet() : null);
		atual.setDescricao(form.getDescricao());
		// Imagens: só troca a que veio preenchida (arquivo novo enviado). Vazio/nulo significa
		// "não alterada", então a imagem já salva no produto é mantida.
		atual.setImagemFrente(manterSeVazio(form.getImagemFrente(), atual.getImagemFrente()));
		atual.setImagemCostas(manterSeVazio(form.getImagemCostas(), atual.getImagemCostas()));
		atual.setImagemModelo(manterSeVazio(form.getImagemModelo(), atual.getImagemModelo()));
		atual.setImagemGuiaTamanhos(manterSeVazio(form.getImagemGuiaTamanhos(), atual.getImagemGuiaTamanhos()));
		atual.getEstoque().clear();
		atual.getEstoque().putAll(estoque);
		return salvarProduto(atual);
	}

	// Valor novo se informado; senão o que já estava salvo
	private String manterSeVazio(String novo, String atual) {
		return (novo == null || novo.isBlank()) ? atual : novo;
	}

	// Mantém só o estoque dos tamanhos marcados (quantidade vazia ou negativa é ignorada/zerada)
	private Map<String, Integer> estoqueDosTamanhosMarcados(Produto form) {
		Map<String, Integer> resultado = new LinkedHashMap<>();
		if (form.getEstoque() == null) {
			return resultado;
		}
		for (String t : form.getTamanhosDisponiveis()) {
			Integer q = form.getEstoque().get(t);
			if (q != null) {
				resultado.put(t, Math.max(q, 0));
			}
		}
		return resultado;
	}

	/** Conta uma visita à página do produto e devolve o produto já atualizado. */
	@Transactional
	public Produto registrarVisualizacao(Long id) throws Exception {
		produtoRepository.incrementarVisualizacoes(id);
		return buscarPorId(id);
	}
}
