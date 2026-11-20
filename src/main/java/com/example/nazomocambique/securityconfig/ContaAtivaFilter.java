package com.example.nazomocambique.securityconfig;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.nazomocambique.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Derruba a sessão de quem foi desativado pelo administrador. Sem isto, a desativação só valeria no próximo
 * login (o Spring confere "ativo" apenas ao autenticar) e a pessoa seguiria usando a sessão que já estava aberta.
 * Registrado só na cadeia do Spring Security (SecurityConfig), por isso não é um @Component.
 */
public class ContaAtivaFilter extends OncePerRequestFilter {

	/** Arquivos estáticos não precisam da checagem (evita uma consulta por imagem/CSS). */
	private static final List<String> PREFIXOS_ESTATICOS = List.of("/css/", "/img/", "/brand/", "/logo-site/",
			"/jshome.js", "/mapa-frete.js", "/favicon.ico");

	private final UsuarioRepository usuarioRepository;

	public ContaAtivaFilter(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String caminho = request.getRequestURI().substring(request.getContextPath().length());
		return PREFIXOS_ESTATICOS.stream().anyMatch(caminho::startsWith);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
				&& usuarioRepository.findByUsername(auth.getName()).map(u -> !u.getAtivo()).orElse(false)) {
			new SecurityContextLogoutHandler().logout(request, response, auth);
			response.sendRedirect(request.getContextPath() + "/login?desativada=true");
			return;
		}
		chain.doFilter(request, response);
	}
}
