package com.example.nazomocambique.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.nazomocambique.service.PedidoService;
import com.example.nazomocambique.service.PrazoEntregaService;

/** Páginas institucionais do rodapé (públicas). */
@Controller
public class InstitucionalController {

	private final PrazoEntregaService prazoEntregaService;

	public InstitucionalController(PrazoEntregaService prazoEntregaService) {
		this.prazoEntregaService = prazoEntregaService;
	}

	// Valores de frete vêm da mesma regra da finalização da compra, então a página nunca fica desatualizada
	@ModelAttribute("freteGratisSul")
	public BigDecimal freteGratisSul() {
		return PedidoService.FRETE_GRATIS_SUL;
	}

	@ModelAttribute("freteGratisDemais")
	public BigDecimal freteGratisDemais() {
		return PedidoService.FRETE_GRATIS_DEMAIS;
	}

	@ModelAttribute("valorFrete")
	public BigDecimal valorFrete() {
		return PedidoService.VALOR_FRETE;
	}

	@GetMapping("/sobre")
	public String sobre(Model model) {
		model.addAttribute("paginaInstitucional", "sobre");
		return "institucional/sobre";
	}

	@GetMapping("/frete-e-entrega")
	public String frete(Model model) {
		model.addAttribute("paginaInstitucional", "frete");
		model.addAttribute("prazos", prazoEntregaService.listar());
		model.addAttribute("ufsSul", String.join(",", PedidoService.UFS_SUL));
		return "institucional/frete-e-entrega";
	}

	@GetMapping("/trocas-e-devolucoes")
	public String trocas(Model model) {
		model.addAttribute("paginaInstitucional", "trocas");
		return "institucional/trocas-e-devolucoes";
	}

	@GetMapping("/politica-de-privacidade")
	public String privacidade(Model model) {
		model.addAttribute("paginaInstitucional", "privacidade");
		return "institucional/politica-de-privacidade";
	}
}
