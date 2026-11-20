package com.example.nazomocambique.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.example.nazomocambique.model.DadosEntrega;
import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.ItemCarrinho;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.service.CheckoutException;
import com.example.nazomocambique.service.ContaService;
import com.example.nazomocambique.service.PagamentoIndisponivelException;
import com.example.nazomocambique.service.PagamentoMercadoPagoService;
import com.example.nazomocambique.service.PedidoService;

/**
 * Finalização da compra (exige login). O carrinho fica no navegador (localStorage); o botão
 * "Finalizar compra" abre /checkout?item=id:tamanho:quantidade&item=... e o servidor confere
 * cada item com o banco antes de mostrar o resumo e de gravar o pedido.
 */
@Controller
public class CheckoutController {

	private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

	private final PedidoService pedidoService;
	private final ContaService contaService;
	private final PagamentoMercadoPagoService pagamentoService;

	public CheckoutController(PedidoService pedidoService, ContaService contaService,
			PagamentoMercadoPagoService pagamentoService) {
		this.pedidoService = pedidoService;
		this.contaService = contaService;
		this.pagamentoService = pagamentoService;
	}

	@ModelAttribute("formasPagamento")
	public FormaPagamento[] formasPagamento() {
		return FormaPagamento.values();
	}

	@ModelAttribute("ufs")
	public List<String> ufs() {
		return List.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB",
				"PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");
	}

	@GetMapping("/checkout")
	public String checkout(@RequestParam(name = "item", required = false) List<String> item, Principal principal,
			Model model) {
		Usuario usuario = usuarioLogado(principal);
		DadosEntrega dados = new DadosEntrega();
		dados.setNome(usuario.getNome() == null ? "" : usuario.getNome());
		dados.setFormaPagamento(FormaPagamento.PIX.name());

		model.addAttribute("dados", dados);
		model.addAttribute("erros", Map.of());
		model.addAttribute("mensagens", List.of());
		model.addAttribute("resumo", pedidoService.montarResumo(PedidoService.lerItens(item), dados));
		// Só no GET a página pode se recarregar sozinha com o carrinho do navegador
		model.addAttribute("sincronizarCarrinho", item == null || item.isEmpty());
		freteNaTela(model);
		return "checkout";
	}

	@PostMapping("/checkout")
	public String finalizar(@RequestParam(name = "item", required = false) List<String> item,
			@ModelAttribute("dados") DadosEntrega dados, Principal principal, Model model) {
		Usuario usuario = usuarioLogado(principal);
		List<ItemCarrinho> itens = PedidoService.lerItens(item);
		try {
			Pedido pedido = pedidoService.finalizar(usuario.getId(), dados, itens);
			// O pedido já está gravado. Se o Pix falhar, a tela do pedido oferece "Gerar Pix" de novo.
			if (pedido.getFormaPagamento() == FormaPagamento.PIX && pagamentoService.disponivel()) {
				try {
					pagamentoService.gerarPix(pedido.getId(), usuario.getId());
				} catch (RuntimeException e) {
					log.warn("Pedido {} gravado, mas o Pix não foi gerado: {}", pedido.getCodigo(), e.getMessage());
				}
			}
			return "redirect:/pedido/" + pedido.getId() + "?novo=1";
		} catch (CheckoutException e) {
			model.addAttribute("erros", e.getErrosCampos());
			model.addAttribute("mensagens", e.getMensagens());
			model.addAttribute("resumo", pedidoService.montarResumo(itens, dados));
			model.addAttribute("sincronizarCarrinho", false);
			freteNaTela(model);
			return "checkout";
		}
	}

	@GetMapping("/pedido/{id}")
	public String pedido(@PathVariable Long id, @RequestParam(required = false) String novo,
			@RequestParam(required = false) String pix, Principal principal, Model model) {
		Usuario usuario = usuarioLogado(principal);
		Pedido pedido = pedidoService.buscarDoUsuario(id, usuario.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
		model.addAttribute("pedido", pedido);
		model.addAttribute("pedidoNovo", novo != null);
		model.addAttribute("pixDisponivel", pagamentoService.disponivel());
		model.addAttribute("pixErro", pix != null);
		return "pedido";
	}

	/** (Re)gera o Pix do pedido, por exemplo quando o anterior venceu ou quando o Mercado Pago estava fora do ar. */
	@PostMapping("/pedido/{id}/pix")
	public String gerarPix(@PathVariable Long id, Principal principal) {
		Usuario usuario = usuarioLogado(principal);
		try {
			pagamentoService.gerarPix(id, usuario.getId());
			return "redirect:/pedido/" + id;
		} catch (PagamentoIndisponivelException e) {
			log.warn("Não foi possível gerar o Pix do pedido {}: {}", id, e.getMessage());
			return "redirect:/pedido/" + id + "?pix=erro";
		}
	}

	/** Consultado pela tela do pedido a cada poucos segundos: quando o Pix é pago, a página se atualiza sozinha. */
	@GetMapping("/pedido/{id}/status")
	@ResponseBody
	public Map<String, String> statusDoPedido(@PathVariable Long id, Principal principal) {
		Usuario usuario = usuarioLogado(principal);
		Pedido pedido = pedidoService.buscarDoUsuario(id, usuario.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
		return Map.of("status", pedido.getStatus().name());
	}

	// Regras de frete para a tela recalcular frete e total ao trocar o estado (o servidor confere de novo ao confirmar)
	private void freteNaTela(Model model) {
		model.addAttribute("freteGratisSul", PedidoService.FRETE_GRATIS_SUL);
		model.addAttribute("freteGratisDemais", PedidoService.FRETE_GRATIS_DEMAIS);
		model.addAttribute("valorFrete", PedidoService.VALOR_FRETE);
		model.addAttribute("ufsSul", String.join(",", PedidoService.UFS_SUL));
	}

	private Usuario usuarioLogado(Principal principal) {
		return contaService.buscarPorUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
	}
}
