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
import com.example.nazomocambique.service.PagamentoAsaasService;
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
	private final PagamentoAsaasService pagamentoService;
	private final com.example.nazomocambique.service.CarteiraService carteiraService;

	public CheckoutController(PedidoService pedidoService, ContaService contaService,
			PagamentoAsaasService pagamentoService, com.example.nazomocambique.service.CarteiraService carteiraService) {
		this.pedidoService = pedidoService;
		this.contaService = contaService;
		this.pagamentoService = pagamentoService;
		this.carteiraService = carteiraService;
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
		dados.setCpf(usuario.getCpf() == null ? "" : usuario.getCpf());
		dados.setTelefone(usuario.getTelefone() == null ? "" : usuario.getTelefone());
		dados.setCep(usuario.getCep() == null ? "" : usuario.getCep());
		dados.setEndereco(usuario.getEndereco() == null ? "" : usuario.getEndereco());
		dados.setNumero(usuario.getNumero() == null ? "" : usuario.getNumero());
		dados.setComplemento(usuario.getComplemento() == null ? "" : usuario.getComplemento());
		dados.setBairro(usuario.getBairro() == null ? "" : usuario.getBairro());
		dados.setCidade(usuario.getCidade() == null ? "" : usuario.getCidade());
		dados.setUf(usuario.getUf() == null ? "" : usuario.getUf());
		dados.setServicoFrete(usuario.getServicoFretePreferido() == null ? "" : usuario.getServicoFretePreferido());
		dados.setFormaPagamento(FormaPagamento.PIX.name());

		model.addAttribute("dados", dados);
		model.addAttribute("erros", Map.of());
		model.addAttribute("mensagens", List.of());
		model.addAttribute("resumo", pedidoService.montarResumo(PedidoService.lerItens(item), dados));
		model.addAttribute("saldoCarteira", carteiraService.saldo(usuario.getId()));
		// Só no GET a página pode se recarregar sozinha com o carrinho do navegador
		model.addAttribute("sincronizarCarrinho", item == null || item.isEmpty());
		freteNaTela(model);
		return "checkout";
	}

	@PostMapping("/checkout")
	public String finalizar(@RequestParam(name = "item", required = false) List<String> item,
			@ModelAttribute("dados") DadosEntrega dados, @RequestParam(name="usarCashback", defaultValue="false") boolean usarCashback, Principal principal, Model model) {
		Usuario usuario = usuarioLogado(principal);
		List<ItemCarrinho> itens = PedidoService.lerItens(item);
		try {
			Pedido pedido = pedidoService.finalizar(usuario.getId(), dados, itens, usarCashback);
			// O pedido já está gravado. Se o Pix falhar, a tela do pedido oferece "Gerar Pix" de novo.
			if (pedido.getFormaPagamento() == FormaPagamento.PIX && pagamentoService.disponivel()) {
				try {
					pagamentoService.gerarPix(pedido.getId(), usuario.getId());
				} catch (RuntimeException e) {
					log.warn("Pedido {} gravado, mas o Pix não foi gerado: {}", pedido.getCodigo(), e.getMessage(), e);
				}
			}
			return "redirect:/pedido/" + pedido.getId() + "?novo=1";
		} catch (CheckoutException e) {
			model.addAttribute("erros", e.getErrosCampos());
			model.addAttribute("mensagens", e.getMensagens());
			model.addAttribute("resumo", pedidoService.montarResumo(itens, dados));
			model.addAttribute("saldoCarteira", carteiraService.saldo(usuario.getId()));
			model.addAttribute("sincronizarCarrinho", false);
			freteNaTela(model);
			return "checkout";
		}
	}

	@GetMapping("/pedido/{id}")
	public String pedido(@PathVariable Long id, @RequestParam(required = false) String novo,
			@RequestParam(required = false) String pix, Principal principal, Model model) {
		Usuario usuario = usuarioLogado(principal);
		pagamentoService.sincronizarPedido(id, usuario.getId());
		Pedido pedido = pedidoService.buscarDoUsuario(id, usuario.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
		model.addAttribute("pedido", pedido);
		model.addAttribute("pedidoNovo", novo != null);
		model.addAttribute("pixDisponivel", pagamentoService.disponivel());
		model.addAttribute("pixErro", pix != null);
		String qrImagem = null;
		if (pedido.isPixAtivo()) {
			try {
				qrImagem = com.example.nazomocambique.service.QrCode.codificar(pedido.getPixCopiaECola()).paraDataUri(4);
			} catch (RuntimeException e) {
				log.warn("Não foi possível desenhar o QR Code do pedido {}: {}", pedido.getCodigo(), e.getMessage());
			}
		}
		model.addAttribute("pixQrImagem", qrImagem);
		// QR Code Pix estático com o total do pedido. Com o Asaas ligado vale o Pix do gateway (confirmação automática).
		return "pedido";
	}

	/** (Re)gera o Pix do pedido, por exemplo quando o anterior venceu ou quando o Asaas estava fora do ar. */
	@PostMapping("/pedido/{id}/pix")
	public String gerarPix(@PathVariable Long id, Principal principal) {
		Usuario usuario = usuarioLogado(principal);
		try {
			pagamentoService.gerarPix(id, usuario.getId());
			return "redirect:/pedido/" + id;
		} catch (RuntimeException e) {
			log.error("PIX_DIAGNOSTICO: falha ao gerar Pix do pedido {}. Causa: {}", id, e.getMessage(), e);
			return "redirect:/pedido/" + id + "?pix=erro";
		}
	}

	/** Consultado pela tela do pedido a cada poucos segundos: quando o Pix é pago, a página se atualiza sozinha. */
	@GetMapping("/pedido/{id}/status")
	@ResponseBody
	public Map<String, String> statusDoPedido(@PathVariable Long id, Principal principal) {
		Usuario usuario = usuarioLogado(principal);
		// Plano B do webhook: se o aviso do Asaas não chegou, confere o pagamento direto na API (no máx. 1x a cada 15 s)
		pagamentoService.sincronizarPedido(id, usuario.getId());
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
