package com.example.nazomocambique.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.service.GestaoUsuarioService;
import com.example.nazomocambique.service.GestaoUsuarioService.GestaoUsuarioException;
import com.example.nazomocambique.service.GestaoUsuarioService.ResumoUsuario;

/**
 * Aba "Usuários" do administrador: lista, ficha com histórico de pedidos e ativar/desativar conta.
 * Só ADMIN (regra de /admin/** no SecurityConfig, repetida aqui). O POST de status exige token CSRF.
 */
@Controller
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

	private final GestaoUsuarioService gestaoUsuarioService;

	public AdminUsuarioController(GestaoUsuarioService gestaoUsuarioService) {
		this.gestaoUsuarioService = gestaoUsuarioService;
	}

	@GetMapping
	public String listar(Model model, Principal principal) {
		List<ResumoUsuario> usuarios = gestaoUsuarioService.listar(principal.getName());
		long ativos = usuarios.stream().filter(r -> r.getUsuario().getAtivo()).count();
		model.addAttribute("usuarios", usuarios);
		model.addAttribute("totalAtivos", ativos);
		model.addAttribute("totalInativos", usuarios.size() - ativos);
		return "admin/usuario/listar";
	}

	@GetMapping("/{id}")
	public String detalhes(@PathVariable Long id, Model model, Principal principal) {
		model.addAttribute("detalhe", gestaoUsuarioService.detalhar(id, principal.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.")));
		model.addAttribute("statusPedido", StatusPedido.values());
		return "admin/usuario/detalhes";
	}

	/** Botões Ativar/Desativar. "retorno" diz para onde voltar: a lista (padrão) ou a ficha do usuário. */
	@PostMapping("/{id}/status")
	public String alterarStatus(@PathVariable Long id, @RequestParam boolean ativo,
			@RequestParam(defaultValue = "lista") String retorno, Principal principal, RedirectAttributes redirect) {
		String destino = "detalhe".equals(retorno) ? "redirect:/admin/usuarios/" + id : "redirect:/admin/usuarios";
		try {
			boolean mudou = gestaoUsuarioService.alterarAtivo(id, ativo, principal.getName());
			if (mudou) {
				redirect.addFlashAttribute("mensagemOk", ativo ? "Conta ativada." : "Conta desativada.");
			} else {
				redirect.addFlashAttribute("mensagemOk", ativo ? "A conta já estava ativa." : "A conta já estava desativada.");
			}
		} catch (GestaoUsuarioException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		}
		return destino;
	}
}
