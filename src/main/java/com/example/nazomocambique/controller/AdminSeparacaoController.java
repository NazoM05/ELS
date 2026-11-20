package com.example.nazomocambique.controller;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.service.ContaService;
import com.example.nazomocambique.service.GestaoPedidoService;
import com.example.nazomocambique.service.GestaoPedidoService.AtualizacaoPedidoException;

/**
 * Tela de separação (Picking & Packing) em Kanban. A página consulta /admin/separacao/pedidos a cada
 * poucos segundos, então cada compra nova aparece sozinha, sem recarregar.
 * Entregues e cancelados ficam fora do quadro (continuam na aba "Pedidos").
 *
 * Acesso: somente ADMIN e GESTOR. A regra está no SecurityConfig (URL) e repetida aqui (método),
 * para que a tela e os dados continuem protegidos mesmo se a regra de URL for alterada por engano.
 */
@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
@RequestMapping("/admin/separacao")
public class AdminSeparacaoController {

	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final List<StatusPedido> ETAPAS = List.of(StatusPedido.PAGO, StatusPedido.EM_SEPARACAO,
			StatusPedido.SEPARADO, StatusPedido.ENVIADO);

	private final GestaoPedidoService gestaoPedidoService;
	private final ContaService contaService;

	public AdminSeparacaoController(GestaoPedidoService gestaoPedidoService, ContaService contaService) {
		this.gestaoPedidoService = gestaoPedidoService;
		this.contaService = contaService;
	}

	public record CartaoPedido(String codigo, String cliente, String destino, String confirmadoEm, String confirmadoEmTexto,
			String status, String statusRotulo, String statusClasse, int itens) {
	}

	@GetMapping
	public String tela() {
		return "admin/pedido/separacao";
	}

	@GetMapping("/pedidos")
	@ResponseBody
	public List<CartaoPedido> pedidos() {
		return gestaoPedidoService.listarTodos().stream()
				.filter(p -> p.getStatus() != StatusPedido.ENTREGUE && p.getStatus() != StatusPedido.CANCELADO)
				.map(AdminSeparacaoController::cartao)
				.toList();
	}

	@PostMapping("/{codigo}/status")
	@ResponseBody
	public ResponseEntity<Map<String, String>> mudarStatus(@PathVariable String codigo, @RequestParam String status,
			Principal principal) {
		StatusPedido novo = StatusPedido.deTexto(status);
		if (novo == null || !ETAPAS.contains(novo)) {
			return ResponseEntity.badRequest().body(Map.of("erro", "Etapa inválida."));
		}
		try {
			Pedido atual = gestaoPedidoService.buscarPorCodigo(codigo).orElse(null);
			if (atual == null) {
				return ResponseEntity.status(404).body(Map.of("erro", "Pedido não encontrado."));
			}
			gestaoPedidoService.atualizar(codigo, novo, atual.getCodigoRastreio(), "Alterado na tela de separação.",
					responsavel(principal), principal.getName());
			return ResponseEntity.ok(Map.of("status", novo.name()));
		} catch (AtualizacaoPedidoException e) {
			return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
		}
	}

	private static CartaoPedido cartao(Pedido p) {
		String destino = (p.getCidade() == null ? "" : p.getCidade()) + (p.getUf() == null ? "" : "/" + p.getUf());
		return new CartaoPedido(p.getCodigo(), p.getNomeDestinatario(), destino,
				p.getCriadoEm() == null ? null : p.getCriadoEm().format(ISO), p.getCriadoEmFormatado(),
				p.getStatus().name(), p.getStatus().getRotulo(), p.getStatus().getClasse(), p.getQuantidadeTotal());
	}

	private String responsavel(Principal principal) {
		return contaService.buscarPorUsername(principal.getName())
				.map(u -> u.getNome() != null && !u.getNome().isBlank() ? u.getNome() : u.getUsername())
				.orElse(principal.getName());
	}
}
