package com.example.nazomocambique.securityconfig;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

import com.example.nazomocambique.repository.UsuarioRepository;
import com.example.nazomocambique.service.AcessoUsuarioService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	/** Perfis (sem o prefixo ROLE_) que podem abrir a tela de separação e ler/alterar os dados dela. */
	public static final String[] PERFIS_SEPARACAO = { "ADMIN", "GESTOR" };

	/**
	 * Quem está logado mas não tem o perfil certo: páginas vão para /acesso-negado; chamadas de dados
	 * (JSON ou POST, como o quadro da separação) recebem 403 com uma mensagem curta.
	 */
	static AccessDeniedHandler acessoNegado() {
		return (request, response, e) -> {
			String accept = request.getHeader("Accept");
			boolean chamadaDeDados = !"GET".equalsIgnoreCase(request.getMethod())
					|| (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
			if (chamadaDeDados) {
				response.setStatus(403);
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write("{\"erro\":\"Acesso negado.\"}");
			} else {
				response.sendRedirect(request.getContextPath() + "/acesso-negado");
			}
		};
	}
	
	/**
	 * Depois do login: o gestor (que não é admin) vai direto para a separação de pedidos, a única área dele;
	 * os demais seguem para a página que tentaram abrir ou, se não houve, para o início.
	 */
	static AuthenticationSuccessHandler aposLogin() {
		SavedRequestAwareAuthenticationSuccessHandler padrao = new SavedRequestAwareAuthenticationSuccessHandler();
		padrao.setDefaultTargetUrl("/");
		return (request, response, authentication) -> {
			boolean gestor = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_GESTOR".equals(a.getAuthority()));
			boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
			if (gestor && !admin) {
				response.sendRedirect(request.getContextPath() + "/admin/separacao");
				return;
			}
			padrao.onAuthenticationSuccess(request, response, authentication);
		};
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarioRepository) {
		http
		// CSRF ligado para as ações da tela de separação e da gestão de usuários: sem isso, um site de
		// terceiros poderia mudar a etapa de um pedido ou desativar uma conta usando a sessão de um admin logado.
		// O resto do site continua sem CSRF, como antes (fora do escopo desta correção).
		.csrf(csrf -> csrf.requireCsrfProtectionMatcher(RequestMatchers.anyOf(
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/admin/separacao/**"),
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/admin/usuarios/**"))))
		// Conta desativada pelo admin perde a sessão aberta (antes da checagem de permissões)
		.addFilterBefore(new ContaAtivaFilter(usuarioRepository), AuthorizationFilter.class)
		.authorizeHttpRequests(auth -> auth 
				// Públicas: login/cadastro, a vitrine (home, busca e detalhes do produto) e os arquivos que ela usa
				.requestMatchers("/login", "/criar-conta", "/acesso-negado").permitAll()
				.requestMatchers("/", "/home", "/london", "/outlet", "/pesquisa", "/detalhes/**", "/favoritos").permitAll()
				.requestMatchers("/sobre", "/frete-e-entrega", "/trocas-e-devolucoes", "/politica-de-privacidade").permitAll()
				.requestMatchers("/css/**", "/jshome.js", "/mapa-frete.js", "/img/**", "/brand/**", "/logo-site/**", "/favicon.ico").permitAll()
				// Webhook do Mercado Pago: sem login (quem chama é o Mercado Pago); a assinatura é conferida no controller
				.requestMatchers(HttpMethod.POST, "/webhooks/mercadopago").permitAll()
				.requestMatchers(PathRequest.toH2Console()).hasRole("ADMIN")
				// Separação de pedidos: somente ADMIN e GESTOR (precisa vir antes da regra geral de /admin/**)
				.requestMatchers("/admin/separacao", "/admin/separacao/**").hasAnyRole(PERFIS_SEPARACAO)
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
				.anyRequest().authenticated()
				
		  )
		 .exceptionHandling(ex -> ex.accessDeniedHandler(acessoNegado()))
		 .formLogin(form -> form
	    			.loginPage("/login")
	    			.loginProcessingUrl("/perform_login")
	    			.successHandler(aposLogin())
	                .failureUrl("/login?error=true")
	    			.permitAll()
	    		)
	            .logout(logout -> logout
	                // O menu usa um link (GET) para sair; com o CSRF ligado o Spring passaria a aceitar só POST
	                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/logout"))
	                .logoutSuccessUrl("/login?logout=true")
	                .permitAll()
	            )
	            .headers(headers -> headers
	                .frameOptions(frame -> frame.sameOrigin())
	            )
	            .httpBasic(basic -> basic.disable());
		
		return http.build();
	}
	
	@Bean 
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	AuthenticationManager authenticationManager(AcessoUsuarioService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
	
	
	void geraSenha() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		IO.println(encoder.encode("senha"));
	}
} 
