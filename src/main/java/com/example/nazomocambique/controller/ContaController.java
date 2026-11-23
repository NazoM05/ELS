package com.example.nazomocambique.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.securityconfig.AcessoUsuario;
import com.example.nazomocambique.service.ContaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Criar conta (pública) e perfil do usuário logado (alterar e-mail e senha). */
@Controller
public class ContaController {

	private static final SecurityContextRepository REPOSITORIO_SESSAO = new HttpSessionSecurityContextRepository();

	private final ContaService contaService;

	public ContaController(ContaService contaService) {
		this.contaService = contaService;
	}

	// ---------------------------------------------------------------- criar conta

	@GetMapping("/criar-conta")
	public String criarContaForm(Model model) {
		model.addAttribute("erros", Map.of());
		return "criar-conta";
	}

	@PostMapping("/criar-conta")
	public String criarConta(@RequestParam(defaultValue = "") String nome,
			@RequestParam(defaultValue = "") String email,
			@RequestParam(defaultValue = "") String senha,
			@RequestParam(defaultValue = "") String confirmarSenha, Model model) {

		Map<String, String> erros = contaService.cadastrar(nome, email, senha, confirmarSenha);
		if (!erros.isEmpty()) {
			// devolve o que foi digitado (menos as senhas) para a pessoa corrigir sem redigitar tudo
			model.addAttribute("erros", erros);
			model.addAttribute("nome", nome.trim());
			model.addAttribute("email", email.trim());
			return "criar-conta";
		}
		return "redirect:/login?conta=criada";
	}

	// ---------------------------------------------------------------- perfil

	@GetMapping("/perfil")
	public String perfil(Principal principal, Model model) {
		carregarPerfil(principal, model);
		return "perfil";
	}

	@PostMapping("/perfil/email")
	public String alterarEmail(Principal principal, @RequestParam(defaultValue = "") String novoEmail,
			@RequestParam(defaultValue = "") String senhaAtual, HttpServletRequest request,
			HttpServletResponse response, Model model, RedirectAttributes redirect) {

		Usuario usuario = usuarioLogado(principal);
		Map<String, String> erros = contaService.alterarEmail(usuario.getId(), senhaAtual, novoEmail);
		if (!erros.isEmpty()) {
			carregarPerfil(principal, model);
			model.addAttribute("errosEmail", erros);
			model.addAttribute("novoEmail", novoEmail.trim());
			return "perfil";
		}

		// O login do usuário mudou: a sessão atual precisa passar a usar o e-mail novo
		atualizarSessao(contaService.buscarPorId(usuario.getId()), request, response);
		redirect.addFlashAttribute("sucessoEmail", "E-mail atualizado com sucesso. Use o novo e-mail na próxima vez que entrar.");
		return "redirect:/perfil";
	}

	@PostMapping("/perfil/dados")
	public String alterarDados(Principal principal, @RequestParam(defaultValue = "") String cpf,
			@RequestParam(defaultValue = "") String dataNascimento, Model model, RedirectAttributes redirect) {

		Usuario usuario = usuarioLogado(principal);
		Map<String, String> erros = contaService.alterarDadosPessoais(usuario.getId(), cpf, dataNascimento);
		if (!erros.isEmpty()) {
			carregarPerfil(principal, model);
			model.addAttribute("errosDados", erros);
			model.addAttribute("cpf", cpf.trim());
			model.addAttribute("dataNascimento", dataNascimento.trim());
			return "perfil";
		}
		redirect.addFlashAttribute("sucessoDados", "Dados pessoais salvos com sucesso.");
		return "redirect:/perfil";
	}

	@PostMapping("/perfil/senha")
	public String alterarSenha(Principal principal, @RequestParam(defaultValue = "") String senhaAtual,
			@RequestParam(defaultValue = "") String novaSenha,
			@RequestParam(defaultValue = "") String confirmarNovaSenha, HttpServletRequest request,
			HttpServletResponse response, Model model, RedirectAttributes redirect) {

		Usuario usuario = usuarioLogado(principal);
		Map<String, String> erros = contaService.alterarSenha(usuario.getId(), senhaAtual, novaSenha, confirmarNovaSenha);
		if (!erros.isEmpty()) {
			carregarPerfil(principal, model);
			model.addAttribute("errosSenha", erros);
			return "perfil";
		}

		atualizarSessao(contaService.buscarPorId(usuario.getId()), request, response);
		redirect.addFlashAttribute("sucessoSenha", "Senha alterada com sucesso.");
		return "redirect:/perfil";
	}

	// ---------------------------------------------------------------- apoio

	private Usuario usuarioLogado(Principal principal) {
		return contaService.buscarPorUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
	}

	private void carregarPerfil(Principal principal, Model model) {
		model.addAttribute("usuario", usuarioLogado(principal));
		if (!model.containsAttribute("errosEmail")) {
			model.addAttribute("errosEmail", Map.of());
		}
		if (!model.containsAttribute("errosDados")) {
			model.addAttribute("errosDados", Map.of());
		}
		if (!model.containsAttribute("errosSenha")) {
			model.addAttribute("errosSenha", Map.of());
		}
	}

	/** Troca o usuário guardado na sessão pelo registro atualizado, sem pedir novo login. */
	private void atualizarSessao(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
		AcessoUsuario atualizado = new AcessoUsuario(usuario);
		Authentication autenticacao = UsernamePasswordAuthenticationToken.authenticated(atualizado, null,
				atualizado.getAuthorities());
		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(autenticacao);
		SecurityContextHolder.setContext(contexto);
		REPOSITORIO_SESSAO.saveContext(contexto, request, response);
	}
}
