package com.example.nazomocambique.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.service.CodigoPedido;
import com.example.nazomocambique.service.ContaService;
import com.example.nazomocambique.service.GestaoPedidoService;
import com.example.nazomocambique.service.GestaoPedidoService.AtualizacaoPedidoException;

/** Aba "Pedidos" do administrador (/admin/** já exige ROLE_ADMIN no SecurityConfig). */
@Controller
@RequestMapping("/admin/pedidos")
public class AdminPedidoController {

	private final GestaoPedidoService gestaoPedidoService;
	private final ContaService contaService;

	public AdminPedidoController(GestaoPedidoService gestaoPedidoService, ContaService contaService) {
		this.gestaoPedidoService = gestaoPedidoService;
		this.contaService = contaService;
	}

	@ModelAttribute("statusPedido")
	public StatusPedido[] statusPedido() {
		return StatusPedido.values();
	}

	@GetMapping
	public String listar(Model model) {
		List<Pedido> pedidos = gestaoPedidoService.listarTodos();
		model.addAttribute("pedidos", pedidos);
		model.addAttribute("contagem", GestaoPedidoService.contarPorStatus(pedidos));
		model.addAttribute("emAberto", pedidos.stream().filter(p -> p.getStatus().isEmAberto()).count());
		return "admin/pedido/listar";
	}

	/** Busca exata pelo código (o campo do topo): achou, abre o pedido; senão volta com aviso. */
	@GetMapping("/buscar")
	public String buscar(@RequestParam(required = false) String codigo, RedirectAttributes redirect) {
		return gestaoPedidoService.buscarPorCodigo(codigo)
				.map(p -> "redirect:/admin/pedidos/" + p.getCodigo())
				.orElseGet(() -> {
					redirect.addAttribute("naoEncontrado", CodigoPedido.normalizar(codigo));
					return "redirect:/admin/pedidos";
				});
	}

	@GetMapping("/{codigo}")
	public String detalhes(@PathVariable String codigo, Model model) {
		Pedido pedido = gestaoPedidoService.buscarPorCodigo(codigo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
		model.addAttribute("pedido", pedido);
		return "admin/pedido/detalhes";
	}

	@PostMapping("/{codigo}/atualizar")
	public String atualizar(@PathVariable String codigo, @RequestParam(required = false) String status,
			@RequestParam(required = false) String codigoRastreio, @RequestParam(required = false) String observacao,
			Principal principal, RedirectAttributes redirect) {
		String destino = "redirect:/admin/pedidos/" + UriUtils.encodePathSegment(CodigoPedido.normalizar(codigo), "UTF-8");
		try {
			boolean mudou = gestaoPedidoService.atualizar(codigo, StatusPedido.deTexto(status), codigoRastreio, observacao,
					responsavel(principal), principal.getName());
			redirect.addAttribute(mudou ? "sucesso" : "semMudanca", "");
		} catch (AtualizacaoPedidoException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
			redirect.addFlashAttribute("observacaoDigitada", observacao);
		}
		return destino;
	}

	private String responsavel(Principal principal) {
		return contaService.buscarPorUsername(principal.getName())
				.map(u -> u.getNome() != null && !u.getNome().isBlank() ? u.getNome() : u.getUsername())
				.orElse(principal.getName());
	}
}
