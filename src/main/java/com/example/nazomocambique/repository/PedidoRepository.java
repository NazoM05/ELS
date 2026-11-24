package com.example.nazomocambique.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);

	List<Pedido> findAllByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

	Optional<Pedido> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);

	/** Trava o pedido para dois avisos do Asaas ao mesmo tempo não o confirmarem duas vezes. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.codigo = :codigo")
	Optional<Pedido> buscarParaAtualizar(@Param("codigo") String codigo);

	List<Pedido> findAllByOrderByCriadoEmDescIdDesc();

	/** Pedidos Pix que já têm cobrança no Asaas e ainda esperam pagamento (tarefa de sincronização). */
	List<Pedido> findAllByStatusAndFormaPagamentoAndAsaasPaymentIdIsNotNullAndCriadoEmAfter(StatusPedido status,
			FormaPagamento formaPagamento, LocalDateTime desde);

	// ---- Gestão de usuários (admin): totais por cliente numa consulta só, sem carregar os pedidos

	/** Quantidade de pedidos e data do último, por cliente. */
	@Query("select p.usuario.id as usuarioId, count(p) as totalPedidos, max(p.criadoEm) as ultimoPedidoEm "
			+ "from Pedido p group by p.usuario.id")
	List<ResumoPedidosUsuario> resumirPorUsuario();

	/** Soma dos totais dos pedidos nas etapas informadas, por cliente. */
	@Query("select p.usuario.id as usuarioId, sum(p.total) as totalGasto "
			+ "from Pedido p where p.status in :status group by p.usuario.id")
	List<GastoUsuario> somarGastoPorUsuario(@Param("status") Collection<StatusPedido> status);

	interface ResumoPedidosUsuario {
		Long getUsuarioId();

		Long getTotalPedidos();

		LocalDateTime getUltimoPedidoEm();
	}

	interface GastoUsuario {
		Long getUsuarioId();

		BigDecimal getTotalGasto();
	}
}
