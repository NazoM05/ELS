package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;

/**
 * Gestão de usuários do administrador: lista com totais de compra, ficha de cada usuário (dados, resumo e
 * histórico de pedidos) e ativar/desativar contas.
 * Conta desativada não consegue entrar (AcessoUsuario.isEnabled) e perde a sessão aberta (ContaAtivaFilter).
 */
@Service
public class GestaoUsuarioService {

	private static final String PERFIL_ADMIN = "ROLE_ADMIN";
	private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	/** Pedidos que contam como compra feita: já pagos e não cancelados. */
	private static final Set<StatusPedido> STATUS_COM_PAGAMENTO = Collections.unmodifiableSet(
			EnumSet.complementOf(EnumSet.of(StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.CANCELADO)));

	private final UsuarioRepository usuarioRepository;
	private final PedidoRepository pedidoRepository;
	private final ContaService contaService;

	public GestaoUsuarioService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository,
			ContaService contaService) {
		this.contaService = contaService;
		this.usuarioRepository = usuarioRepository;
		this.pedidoRepository = pedidoRepository;
	}

	/** Todos os usuários em ordem alfabética, cada um com a quantidade de pedidos e o total gasto. */
	@Transactional(readOnly = true)
	public List<ResumoUsuario> listar(String loginAdmin) {
		Map<Long, PedidoRepository.ResumoPedidosUsuario> pedidos = new HashMap<>();
		pedidoRepository.resumirPorUsuario().forEach(r -> pedidos.put(r.getUsuarioId(), r));
		Map<Long, BigDecimal> gastos = new HashMap<>();
		pedidoRepository.somarGastoPorUsuario(STATUS_COM_PAGAMENTO)
				.forEach(g -> gastos.put(g.getUsuarioId(), g.getTotalGasto()));
		long adminsAtivos = contarAdminsAtivos();

		return contaService.listar().stream()
				.sorted(Comparator.comparing((Usuario u) -> u.getNomeExibicao() == null ? "" : u.getNomeExibicao().toLowerCase(Locale.ROOT)))
				.map(u -> {
					PedidoRepository.ResumoPedidosUsuario resumo = pedidos.get(u.getId());
					return new ResumoUsuario(u, resumo == null ? 0L : resumo.getTotalPedidos(),
							gastos.getOrDefault(u.getId(), BigDecimal.ZERO), resumo == null ? null : resumo.getUltimoPedidoEm(),
							motivoBloqueio(u, loginAdmin, adminsAtivos));
				}).toList();
	}

	/** Ficha completa do usuário; vazio quando o id não existe. */
	@Transactional(readOnly = true)
	public Optional<DetalheUsuario> detalhar(Long id, String loginAdmin) {
		return usuarioRepository.findById(id).map(usuario -> {
			List<Pedido> pedidos = pedidoRepository.findAllByUsuarioIdOrderByCriadoEmDesc(usuario.getId());
			BigDecimal gasto = BigDecimal.ZERO;
			long pagos = 0;
			for (Pedido p : pedidos) {
				if (STATUS_COM_PAGAMENTO.contains(p.getStatus())) {
					gasto = gasto.add(p.getTotal());
					pagos++;
				}
			}
			BigDecimal ticketMedio = pagos == 0 ? null : gasto.divide(BigDecimal.valueOf(pagos), 2, RoundingMode.HALF_UP);
			return new DetalheUsuario(usuario, pedidos, gasto, ticketMedio, GestaoPedidoService.contarPorStatus(pedidos),
					motivoBloqueio(usuario, loginAdmin, contarAdminsAtivos()));
		});
	}

	public long contarAdminsAtivos() {
		return usuarioRepository.countByRoleAndAtivoTrue(PERFIL_ADMIN);
	}

	/**
	 * Ativa ou desativa a conta. Não deixa o admin se desativar nem desativar o último administrador ativo
	 * (a loja ficaria sem ninguém para gerenciá-la).
	 *
	 * @return true se a situação mudou; false se a conta já estava assim
	 */
	@Transactional
	public boolean alterarAtivo(Long id, boolean ativo, String loginAdmin) {
		Usuario alvo = usuarioRepository.findById(id).orElseThrow(() -> new GestaoUsuarioException("Usuário não encontrado."));
		if (alvo.getAtivo() == ativo) {
			return false;
		}
		if (!ativo) {
			String motivo = motivoBloqueio(alvo, loginAdmin, contarAdminsAtivos());
			if (motivo != null) {
				throw new GestaoUsuarioException(motivo);
			}
		}
		alvo.setAtivo(ativo);
		usuarioRepository.save(alvo);
		return true;
	}

	/** Por que esta conta não pode ser desativada agora; null quando pode (ou quando já está desativada). */
	private static String motivoBloqueio(Usuario alvo, String loginAdmin, long adminsAtivos) {
		if (!alvo.getAtivo()) {
			return null;
		}
		if (alvo.getUsername() != null && alvo.getUsername().equalsIgnoreCase(loginAdmin)) {
			return "Você não pode desativar a própria conta.";
		}
		if (PERFIL_ADMIN.equals(alvo.getRole()) && adminsAtivos <= 1) {
			return "Não é possível desativar o último administrador ativo.";
		}
		return null;
	}

	/** Uma linha da lista de usuários. */
	public static class ResumoUsuario {

		private final Usuario usuario;
		private final long totalPedidos;
		private final BigDecimal totalGasto;
		private final LocalDateTime ultimoPedidoEm;
		private final String bloqueioDesativar;

		ResumoUsuario(Usuario usuario, long totalPedidos, BigDecimal totalGasto, LocalDateTime ultimoPedidoEm,
				String bloqueioDesativar) {
			this.usuario = usuario;
			this.totalPedidos = totalPedidos;
			this.totalGasto = totalGasto;
			this.ultimoPedidoEm = ultimoPedidoEm;
			this.bloqueioDesativar = bloqueioDesativar;
		}

		public Usuario getUsuario() {
			return usuario;
		}

		public long getTotalPedidos() {
			return totalPedidos;
		}

		public BigDecimal getTotalGasto() {
			return totalGasto;
		}

		public LocalDateTime getUltimoPedidoEm() {
			return ultimoPedidoEm;
		}

		public String getUltimoPedidoEmFormatado() {
			return ultimoPedidoEm == null ? null : ultimoPedidoEm.format(FORMATO_DATA);
		}

		/** Motivo de o botão "Desativar" estar bloqueado; null quando liberado. */
		public String getBloqueioDesativar() {
			return bloqueioDesativar;
		}
	}

	/** Ficha do usuário: dados, resumo de compras e histórico de pedidos (mais recente primeiro). */
	public static class DetalheUsuario {

		private final Usuario usuario;
		private final List<Pedido> pedidos;
		private final BigDecimal totalGasto;
		private final BigDecimal ticketMedio;
		private final Map<StatusPedido, Long> contagemPorStatus;
		private final String bloqueioDesativar;

		DetalheUsuario(Usuario usuario, List<Pedido> pedidos, BigDecimal totalGasto, BigDecimal ticketMedio,
				Map<StatusPedido, Long> contagemPorStatus, String bloqueioDesativar) {
			this.usuario = usuario;
			this.pedidos = pedidos;
			this.totalGasto = totalGasto;
			this.ticketMedio = ticketMedio;
			this.contagemPorStatus = contagemPorStatus;
			this.bloqueioDesativar = bloqueioDesativar;
		}

		public Usuario getUsuario() {
			return usuario;
		}

		public List<Pedido> getPedidos() {
			return pedidos;
		}

		public int getTotalPedidos() {
			return pedidos.size();
		}

		public BigDecimal getTotalGasto() {
			return totalGasto;
		}

		/** Média dos pedidos pagos; null quando ainda não há nenhum. */
		public BigDecimal getTicketMedio() {
			return ticketMedio;
		}

		public Map<StatusPedido, Long> getContagemPorStatus() {
			return contagemPorStatus;
		}

		public long getPedidosEmAberto() {
			return pedidos.stream().filter(p -> p.getStatus().isEmAberto()).count();
		}

		/** O pedido mais recente; null quando não há pedidos. */
		public Pedido getUltimoPedido() {
			return pedidos.isEmpty() ? null : pedidos.get(0);
		}

		public String getBloqueioDesativar() {
			return bloqueioDesativar;
		}
	}

	/** Regra de negócio violada; a mensagem é mostrada ao administrador. */
	public static class GestaoUsuarioException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public GestaoUsuarioException(String mensagem) {
			super(mensagem);
		}
	}
}
