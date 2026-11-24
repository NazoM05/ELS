package com.example.nazomocambique.controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.service.ContaService;
import com.example.nazomocambique.service.PagamentoAsaasService;
import com.example.nazomocambique.service.PedidoService;

/**
 * Área do usuário: "Meus pedidos" reúne todos os pedidos da conta logada, com filtro por situação.
 * Cada pedido abre em /pedido/{id} (detalhes, Pix, rastreio). O SEPARADOR não entra aqui.
 */
@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'USUARIO')")
public class MeusPedidosController {

	/** Quantos pedidos "aguardando pagamento" são conferidos no Asaas a cada abertura da lista. */
	private static final int MAX_CONFERENCIAS_PIX = 5;

	private final PedidoService pedidoService;
	private final ContaService contaService;
	private final PagamentoAsaasService pagamentoService;

	public MeusPedidosController(PedidoService pedidoService, ContaService contaService,
			PagamentoAsaasService pagamentoService) {
		this.pedidoService = pedidoService;
		this.contaService = contaService;
		this.pagamentoService = pagamentoService;
	}

	/** Filtros da tela. O parâmetro da URL é o nome em minúsculas (?filtro=andamento). */
	public enum Filtro {
		TODOS("Todos"), ANDAMENTO("Em andamento"), ENTREGUES("Entregues"), CANCELADOS("Cancelados");

		private final String rotulo;

		Filtro(String rotulo) {
			this.rotulo = rotulo;
		}

		public String getRotulo() {
			return rotulo;
		}

		/** Texto da URL (ex.: "andamento"). */
		public String getParametro() {
			return name().toLowerCase(Locale.ROOT);
		}

		public boolean aceita(Pedido p) {
			StatusPedido s = p.getStatus();
			return switch (this) {
				case TODOS -> true;
				case ANDAMENTO -> s != StatusPedido.ENTREGUE && s != StatusPedido.CANCELADO;
				case ENTREGUES -> s == StatusPedido.ENTREGUE;
				case CANCELADOS -> s == StatusPedido.CANCELADO;
			};
		}

		/** Texto desconhecido ou vazio vira TODOS. */
		public static Filtro deTexto(String texto) {
			if (texto == null || texto.isBlank()) {
				return TODOS;
			}
			try {
				return valueOf(texto.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return TODOS;
			}
		}
	}

	/** Um botão de filtro com a quantidade de pedidos que ele mostra. */
	public record OpcaoFiltro(String parametro, String rotulo, long total, boolean ativo) {
	}

	@GetMapping("/meus-pedidos")
	public String meusPedidos(@RequestParam(name = "filtro", required = false) String filtro, Principal principal,
			Model model) {
		Usuario usuario = contaService.buscarPorUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));

		List<Pedido> todos = pedidoService.listarDoUsuario(usuario.getId());

		// Plano B do webhook: pedidos Pix ainda pendentes são conferidos no Asaas (no máx. 1x a cada 15 s por pedido)
		boolean consultou = false;
		int conferidos = 0;
		for (Pedido p : todos) {
			if (conferidos >= MAX_CONFERENCIAS_PIX) {
				break;
			}
			if (p.isAguardandoPix()) {
				conferidos++;
				consultou |= pagamentoService.sincronizarPedido(p.getId(), usuario.getId());
			}
		}
		if (consultou) {
			todos = pedidoService.listarDoUsuario(usuario.getId());
		}

		Filtro atual = Filtro.deTexto(filtro);
		List<Pedido> pedidos = todos.stream().filter(atual::aceita).toList();
		List<OpcaoFiltro> opcoes = java.util.Arrays.stream(Filtro.values())
				.map(f -> new OpcaoFiltro(f.getParametro(), f.getRotulo(), todos.stream().filter(f::aceita).count(),
						f == atual))
				.toList();

		model.addAttribute("pedidos", pedidos);
		model.addAttribute("opcoesFiltro", opcoes);
		model.addAttribute("filtroAtual", atual);
		model.addAttribute("totalGeral", todos.size());
		model.addAttribute("areaPedidos", true);
		return "meus-pedidos";
	}
}
