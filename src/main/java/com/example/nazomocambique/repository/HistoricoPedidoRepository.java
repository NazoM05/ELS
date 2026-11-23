package com.example.nazomocambique.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.nazomocambique.model.HistoricoPedido;
import com.example.nazomocambique.model.StatusPedido;

/** Consulta da trilha de alterações dos pedidos (tela /admin/historico). Somente leitura. */
public interface HistoricoPedidoRepository extends JpaRepository<HistoricoPedido, Long> {

	String FILTROS = """
			where (:codigo is null or replace(upper(p.codigo), '-', '') like :codigo)
			  and (:usuario is null or lower(h.responsavel) like :usuario or lower(h.usuarioLogin) like :usuario)
			  and (:etapa is null or h.status = :etapa or h.statusAnterior = :etapa)
			  and (cast(:desde as timestamp) is null or h.criadoEm >= :desde)
			  and (cast(:ate as timestamp) is null or h.criadoEm < :ate)
			  and (:soEtapas = false or (h.statusAnterior is not null and h.statusAnterior <> h.status))
			""";

	/**
	 * Filtros opcionais (null = sem filtro). "codigo" e "usuario" já chegam prontos para o LIKE
	 * (código sem hífens em maiúsculas, usuário em minúsculas, ambos com %).
	 */
	@Query(value = "select h from HistoricoPedido h join fetch h.pedido p " + FILTROS + " order by h.criadoEm desc, h.id desc",
			countQuery = "select count(h) from HistoricoPedido h join h.pedido p " + FILTROS)
	Page<HistoricoPedido> buscar(@Param("codigo") String codigo, @Param("usuario") String usuario,
			@Param("etapa") StatusPedido etapa, @Param("desde") LocalDateTime desde, @Param("ate") LocalDateTime ate,
			@Param("soEtapas") boolean soEtapas, Pageable pageable);
}
