
package com.example.nazomocambique.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.repository.ProdutoRepository;

@Service
public class ProdutoService {
   
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
	 * Salva o que veio dos formulários de cadastro e edição.
	 * Na edição, carrega o produto do banco e atualiza só os campos do formulário, para não
	 * perder dados que a tela não envia (vendidas, visualizações, data de criação).
	 */
	@Transactional
	public Produto salvarFormulario(Produto form) throws Exception {
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
		atual.setTamanho(form.getTamanho());
		atual.setStatus(form.isStatus());
		atual.setDescricao(form.getDescricao());
		atual.setImagemFrente(form.getImagemFrente());
		atual.setImagemCostas(form.getImagemCostas());
		atual.getEstoque().clear();
		atual.getEstoque().putAll(estoque);
		return salvarProduto(atual);
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
