package com.example.nazomocambique.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.UsuarioRepository;

/**
 * Regras da conta do cliente: criar conta, alterar e-mail e alterar senha.
 * O e-mail é guardado no campo "username" (é o que o formulário de login usa), sempre em minúsculas.
 * Os métodos de gravação devolvem um mapa campo -> mensagem; mapa vazio significa que deu certo.
 */
@Service
public class ContaService {

	private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
	private static final int EMAIL_MAX = 150;
	private static final int NOME_MIN = 2;
	private static final int NOME_MAX = 80;
	// BCrypt só considera os primeiros 72 bytes; o limite de 64 caracteres deixa folga para acentos
	private static final int SENHA_MIN = 8;
	private static final int SENHA_MAX = 64;

	private static final String MSG_EMAIL_EM_USO = "Este e-mail já está cadastrado. Tente entrar ou use outro e-mail.";
	private static final String MSG_SENHA_REGRA = "A senha deve ter de 8 a 64 caracteres, com pelo menos uma letra e um número.";

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public ContaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public Optional<Usuario> buscarPorUsername(String username) {
		return usuarioRepository.findByUsername(username);
	}

	public Usuario buscarPorId(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
	}

	public static String normalizarEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	// ---------------------------------------------------------------- criar conta

	public Map<String, String> cadastrar(String nome, String email, String senha, String confirmacao) {
		Map<String, String> erros = new LinkedHashMap<>();

		String nomeLimpo = nome == null ? "" : nome.trim();
		if (nomeLimpo.isEmpty()) {
			erros.put("nome", "Informe seu nome.");
		} else if (nomeLimpo.length() < NOME_MIN || nomeLimpo.length() > NOME_MAX) {
			erros.put("nome", "O nome deve ter entre " + NOME_MIN + " e " + NOME_MAX + " caracteres.");
		}

		String emailLimpo = normalizarEmail(email);
		String erroEmail = validarEmail(emailLimpo);
		if (erroEmail != null) {
			erros.put("email", erroEmail);
		} else if (usuarioRepository.existsByUsernameIgnoreCase(emailLimpo)) {
			erros.put("email", MSG_EMAIL_EM_USO);
		}

		validarNovaSenha(senha, confirmacao, "senha", "confirmarSenha", erros);

		if (!erros.isEmpty()) {
			return erros;
		}

		Usuario usuario = new Usuario();
		usuario.setNome(nomeLimpo);
		usuario.setUsername(emailLimpo);
		usuario.setSenha(passwordEncoder.encode(senha));
		usuario.setRole("ROLE_USUARIO");
		usuario.setAtivo(true);
		try {
			usuarioRepository.save(usuario);
		} catch (DataIntegrityViolationException e) {
			// duas pessoas cadastrando o mesmo e-mail ao mesmo tempo (a coluna é única)
			erros.put("email", MSG_EMAIL_EM_USO);
		}
		return erros;
	}

	// ---------------------------------------------------------------- perfil

	public Map<String, String> alterarEmail(Long usuarioId, String senhaAtual, String novoEmail) {
		Map<String, String> erros = new LinkedHashMap<>();
		Usuario usuario = buscarPorId(usuarioId);

		if (!senhaConfere(senhaAtual, usuario)) {
			erros.put("senhaAtual", "Senha atual incorreta.");
		}

		String emailLimpo = normalizarEmail(novoEmail);
		String erroEmail = validarEmail(emailLimpo);
		if (erroEmail != null) {
			erros.put("novoEmail", erroEmail);
		} else if (emailLimpo.equalsIgnoreCase(usuario.getUsername())) {
			erros.put("novoEmail", "Este já é o e-mail da sua conta.");
		} else if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(emailLimpo, usuario.getId())) {
			erros.put("novoEmail", MSG_EMAIL_EM_USO);
		}

		if (!erros.isEmpty()) {
			return erros;
		}

		usuario.setUsername(emailLimpo);
		try {
			usuarioRepository.save(usuario);
		} catch (DataIntegrityViolationException e) {
			erros.put("novoEmail", MSG_EMAIL_EM_USO);
		}
		return erros;
	}

	public Map<String, String> alterarSenha(Long usuarioId, String senhaAtual, String novaSenha, String confirmacao) {
		Map<String, String> erros = new LinkedHashMap<>();
		Usuario usuario = buscarPorId(usuarioId);

		boolean atualOk = senhaConfere(senhaAtual, usuario);
		if (!atualOk) {
			erros.put("senhaAtual", "Senha atual incorreta.");
		}

		validarNovaSenha(novaSenha, confirmacao, "novaSenha", "confirmarNovaSenha", erros);

		if (atualOk && !erros.containsKey("novaSenha") && novaSenha.equals(senhaAtual)) {
			erros.put("novaSenha", "A nova senha deve ser diferente da senha atual.");
		}

		if (!erros.isEmpty()) {
			return erros;
		}

		usuario.setSenha(passwordEncoder.encode(novaSenha));
		usuarioRepository.save(usuario);
		return erros;
	}

	public Map<String, String> alterarDadosPessoais(Long usuarioId, String cpf, String dataNascimento) {
		Map<String, String> erros = new LinkedHashMap<>();
		Usuario usuario = buscarPorId(usuarioId);

		String cpfLimpo = cpf == null ? "" : cpf.replaceAll("\\D", "");
		if (cpfLimpo.isEmpty()) {
			erros.put("cpf", "Informe o CPF.");
		} else if (!cpfValido(cpfLimpo)) {
			erros.put("cpf", "CPF inválido. Confira os números digitados.");
		} else if (usuarioRepository.existsByCpfAndIdNot(cpfLimpo, usuario.getId())) {
			erros.put("cpf", "Este CPF já está cadastrado em outra conta.");
		}

		LocalDate nascimento = null;
		if (dataNascimento == null || dataNascimento.isBlank()) {
			erros.put("dataNascimento", "Informe a data de nascimento.");
		} else {
			try {
				nascimento = LocalDate.parse(dataNascimento.trim()); // formato do campo date: aaaa-mm-dd
				if (nascimento.isAfter(LocalDate.now()) || nascimento.getYear() < 1900) {
					erros.put("dataNascimento", "Informe uma data de nascimento válida.");
				}
			} catch (DateTimeParseException e) {
				erros.put("dataNascimento", "Informe uma data de nascimento válida.");
			}
		}

		if (!erros.isEmpty()) {
			return erros;
		}

		usuario.setCpf(cpfLimpo);
		usuario.setDataNascimento(nascimento);
		try {
			usuarioRepository.save(usuario);
		} catch (DataIntegrityViolationException e) {
			erros.put("cpf", "Este CPF já está cadastrado em outra conta.");
		}
		return erros;
	}

	// ---------------------------------------------------------------- validações

	/** CPF com 11 dígitos, sem sequência repetida e com os dois dígitos verificadores corretos. */
	private boolean cpfValido(String cpf) {
		if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
			return false;
		}
		return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0' && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
	}

	private int digitoVerificador(String cpf, int quantidade) {
		int soma = 0;
		for (int i = 0; i < quantidade; i++) {
			soma += (cpf.charAt(i) - '0') * (quantidade + 1 - i);
		}
		int resto = (soma * 10) % 11;
		return resto == 10 ? 0 : resto;
	}

	/** @return mensagem de erro, ou null quando o e-mail (já normalizado) é válido */
	private String validarEmail(String email) {
		if (email.isEmpty()) {
			return "Informe o e-mail.";
		}
		if (email.length() > EMAIL_MAX || !FORMATO_EMAIL.matcher(email).matches()) {
			return "Informe um e-mail válido, como nome@exemplo.com.";
		}
		return null;
	}

	private void validarNovaSenha(String senha, String confirmacao, String campoSenha, String campoConfirmacao,
			Map<String, String> erros) {
		if (senha == null || senha.isEmpty()) {
			erros.put(campoSenha, "Informe a senha.");
		} else if (!senhaForte(senha)) {
			erros.put(campoSenha, MSG_SENHA_REGRA);
		}

		if (confirmacao == null || confirmacao.isEmpty()) {
			erros.put(campoConfirmacao, "Repita a senha para confirmar.");
		} else if (!erros.containsKey(campoSenha) && !confirmacao.equals(senha)) {
			erros.put(campoConfirmacao, "As senhas não coincidem.");
		}
	}

	private boolean senhaForte(String senha) {
		if (senha.length() < SENHA_MIN || senha.length() > SENHA_MAX) {
			return false;
		}
		boolean temLetra = senha.chars().anyMatch(Character::isLetter);
		boolean temNumero = senha.chars().anyMatch(Character::isDigit);
		return temLetra && temNumero;
	}

	private boolean senhaConfere(String senhaDigitada, Usuario usuario) {
		return senhaDigitada != null && !senhaDigitada.isEmpty() && usuario.getSenha() != null
				&& passwordEncoder.matches(senhaDigitada, usuario.getSenha());
	}
}
