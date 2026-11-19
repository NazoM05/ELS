package com.example.nazomocambique.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.nazomocambique.model.Produto;


public interface ProdutoRepository extends JpaRepository<Produto, Long> {
	Optional<Produto> findByNome(String nome);

	List<Produto> findAllByNomeContainingIgnoreCase(String termoPesquisa);

	// UPDATE direto: não dispara @PreUpdate, então "atualizadoEm" não muda a cada visita à página.
	@Modifying(clearAutomatically = true)
	@Query("update Produto p set p.visualizacoes = coalesce(p.visualizacoes, 0) + 1 where p.id = :id")
	int incrementarVisualizacoes(@Param("id") Long id);
}
