package com.example.nazomocambique.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.Produto;


public interface ProdutoRepository extends JpaRepository<Produto, Long> {
	Optional<Produto> findByNome(String nome);

	List<Produto> findAllByNomeContainingIgnoreCase(String termoPesquisa);

	/**
	 * Catálogo da loja com filtros combináveis: cada parâmetro nulo significa "sem esse filtro".
	 * Gênero usa dois valores (g1 ou g2) para o Unissex aparecer tanto em Masculino quanto em Feminino.
	 * Paginado: a ordem vem do Sort do Pageable (sempre termina em id, para as páginas não se embaralharem).
	 */
	@Query(value = """
			select p from Produto p
			where (:termo is null or lower(p.nome) like lower(concat('%', :termo, '%')))
			  and (:categoria is null or p.categoria = :categoria)
			  and (:g1 is null or p.genero = :g1 or p.genero = :g2)
			""", countQuery = """
			select count(p) from Produto p
			where (:termo is null or lower(p.nome) like lower(concat('%', :termo, '%')))
			  and (:categoria is null or p.categoria = :categoria)
			  and (:g1 is null or p.genero = :g1 or p.genero = :g2)
			""")
	Page<Produto> filtrar(@Param("termo") String termo, @Param("categoria") Categoria categoria,
			@Param("g1") Genero g1, @Param("g2") Genero g2, Pageable pageable);

	// UPDATE direto: não dispara @PreUpdate, então "atualizadoEm" não muda a cada visita à página.
	@Modifying(clearAutomatically = true)
	@Query("update Produto p set p.visualizacoes = coalesce(p.visualizacoes, 0) + 1 where p.id = :id")
	int incrementarVisualizacoes(@Param("id") Long id);
}
