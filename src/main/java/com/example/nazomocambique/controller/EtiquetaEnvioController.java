package com.example.nazomocambique.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.example.nazomocambique.config.CorreiosProperties;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.PrePostagem;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.service.CodigoBarras;
import com.example.nazomocambique.service.CodigoPedido;
import com.example.nazomocambique.service.ContaService;
import com.example.nazomocambique.service.EtiquetaEnvioService;
import com.example.nazomocambique.service.EtiquetaEnvioService.EtiquetaException;
import com.example.nazomocambique.service.FreteService;
import com.example.nazomocambique.service.GestaoPedidoService;

/**
 * Etiqueta de envio dos Correios, para ADMIN e SEPARADOR (a regra de URL em /admin/separacao/** já cobre os dois e
 * o CSRF do POST; o @PreAuthorize repete a regra por método).
 * <ul>
 * <li>POST .../etiqueta: cria a pré-postagem nos Correios (uma vez por pedido);</li>
 * <li>GET .../etiqueta?formato=termica|a4: página de impressão (10x15 cm ou folha A4).</li>
 * </ul>
 */
@Controller
public class EtiquetaEnvioController {

	private final GestaoPedidoService gestaoPedidoService;
	private final EtiquetaEnvioService etiquetaService;
	private final ContaService contaService;
	private final CorreiosProperties props;

	public EtiquetaEnvioController(GestaoPedidoService gestaoPedidoService, EtiquetaEnvioService etiquetaService,
			ContaService contaService, CorreiosProperties props) {
		this.gestaoPedidoService = gestaoPedidoService;
		this.etiquetaService = etiquetaService;
		this.contaService = contaService;
		this.props = props;
	}

	@PostMapping("/admin/separacao/pedido/{codigo}/etiqueta")
	@PreAuthorize("hasAnyRole('ADMIN', 'SEPARADOR')")
	public String gerar(@PathVariable String codigo, Principal principal, RedirectAttributes redirect) {
		String normalizado = CodigoPedido.normalizar(codigo);
		try {
			PrePostagem pre = etiquetaService.gerar(normalizado, responsavel(principal), principal.getName());
			redirect.addFlashAttribute("sucesso", "Etiqueta gerada. Código de rastreamento: " + pre.codigoObjeto() + ".");
		} catch (EtiquetaException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		return "redirect:/admin/separacao/pedido/" + UriUtils.encodePathSegment(normalizado, "UTF-8");
	}

	@GetMapping("/admin/separacao/pedido/{codigo}/etiqueta")
	@PreAuthorize("hasAnyRole('ADMIN', 'SEPARADOR')")
	public String imprimir(@PathVariable String codigo, @RequestParam(defaultValue = "termica") String formato,
			Model model, RedirectAttributes redirect) {
		Pedido pedido = gestaoPedidoService.buscarPorCodigo(codigo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
		if (pedido.getStatus() == StatusPedido.CANCELADO || !pedido.isEtiquetaGerada()) {
			redirect.addFlashAttribute("erro", pedido.getStatus() == StatusPedido.CANCELADO
					? "Pedido cancelado: não imprima a etiqueta."
					: "Gere a etiqueta dos Correios antes de imprimir.");
			return "redirect:/admin/separacao/pedido/" + UriUtils.encodePathSegment(pedido.getCodigo(), "UTF-8");
		}
		String servicoCodigo = EtiquetaEnvioService.codigoServico(pedido);
		CorreiosProperties.Remetente r = props.remetente();
		model.addAttribute("pedido", pedido);
		model.addAttribute("formato", "a4".equalsIgnoreCase(formato) ? "a4" : "termica");
		model.addAttribute("servicoNome", servicoCodigo.isEmpty() ? pedido.getServicoFrete() : FreteService.nomeServico(servicoCodigo));
		model.addAttribute("servicoCodigo", servicoCodigo);
		model.addAttribute("remetente", r);
		model.addAttribute("cepOrigem", formatarCep(props.cepOrigem()));
		model.addAttribute("barrasObjeto", CodigoBarras.svgCodigoObjeto(pedido.getCorreiosCodigoObjeto()));
		model.addAttribute("barrasCep", CodigoBarras.svgCep(pedido.getCep()));
		return "admin/pedido/etiqueta";
	}

	private static String formatarCep(String cep) {
		return cep != null && cep.length() == 8 ? cep.substring(0, 5) + "-" + cep.substring(5) : cep;
	}

	private String responsavel(Principal principal) {
		return contaService.buscarPorUsername(principal.getName())
				.map(u -> u.getNome() != null && !u.getNome().isBlank() ? u.getNome() : u.getUsername())
				.orElse(principal.getName());
	}
}
