package com.example.nazomocambique.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.service.AparenciaService;

/** Tela "Configurações" do painel: cor principal e logo do site. Acesso só para ADMIN (ver SecurityConfig). */
@Controller
@RequestMapping("/admin/configuracoes")
public class ConfiguracaoController {

	private final AparenciaService aparenciaService;

	public ConfiguracaoController(AparenciaService aparenciaService) {
		this.aparenciaService = aparenciaService;
	}

	@GetMapping
	public String tela(Model model) {
		// nome -> hex (o primeiro é o padrão do projeto)
		Map<String, String> cores = new LinkedHashMap<>();
		cores.put("Preto (padrão)", "#0A0A0A");
		cores.put("Vermelho", "#C8102E");
		cores.put("Rosa", "#E0577C");
		cores.put("Laranja", "#E8590C");
		cores.put("Amarelo", "#FACC15");
		cores.put("Verde", "#1F7A4D");
		cores.put("Azul", "#1D4ED8");
		cores.put("Roxo", "#5B2A86");
		model.addAttribute("coresPredefinidas", cores);
		return "admin/configuracoes";
	}

	@PostMapping("/salvar")
	public String salvar(@RequestParam String corPrimaria,
			@RequestParam(value = "logo", required = false) MultipartFile logo,
			RedirectAttributes redirect) {
		try {
			aparenciaService.salvar(corPrimaria, logo);
			redirect.addFlashAttribute("sucesso", "Alterações salvas. A nova aparência já vale para todo o site.");
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			redirect.addFlashAttribute("erro", "Não foi possível salvar as alterações. Tente novamente.");
		}
		return "redirect:/admin/configuracoes";
	}

	@PostMapping("/restaurar")
	public String restaurar(RedirectAttributes redirect) {
		try {
			aparenciaService.restaurarPadrao();
			redirect.addFlashAttribute("sucesso", "Cor e logo originais restaurados.");
		} catch (Exception e) {
			e.printStackTrace();
			redirect.addFlashAttribute("erro", "Não foi possível restaurar o padrão. Tente novamente.");
		}
		return "redirect:/admin/configuracoes";
	}
}
