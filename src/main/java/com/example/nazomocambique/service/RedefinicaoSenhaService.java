package com.example.nazomocambique.service;


import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.CodigoRedefinicaoSenha;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.CodigoRedefinicaoSenhaRepository;
import com.example.nazomocambique.repository.UsuarioRepository;

@Service
public class RedefinicaoSenhaService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VALIDADE_MINUTOS = 15;
    private static final int MAX_TENTATIVAS = 5;
    private static final int INTERVALO_REENVIO_SEGUNDOS = 60;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final UsuarioRepository usuarios;
    private final CodigoRedefinicaoSenhaRepository codigos;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final String remetente;


    public RedefinicaoSenhaService(UsuarioRepository usuarios, CodigoRedefinicaoSenhaRepository codigos,
            PasswordEncoder encoder, JavaMailSender mailSender,
            @Value("${app.mail.from:${spring.mail.username:}}") String remetente) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.remetente = remetente;
    }


    @Transactional
    public void solicitar(String email) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalizado).matches()) return;
        Usuario usuario = usuarios.findByUsername(normalizado).orElse(null);
        // Resposta sempre genérica para não revelar se a conta existe.
        if (usuario == null || !usuario.getAtivo()) return;

        LocalDateTime agora = LocalDateTime.now();
        CodigoRedefinicaoSenha anterior = codigos.findByUsuario(usuario).orElse(null);
        if (anterior != null && anterior.getCriadoEm().isAfter(agora.minusSeconds(INTERVALO_REENVIO_SEGUNDOS))) return;

        codigos.deleteByUsuario(usuario);
        codigos.flush();
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        CodigoRedefinicaoSenha registro = codigos.save(new CodigoRedefinicaoSenha(
                usuario, encoder.encode(codigo), agora, agora.plusMinutes(VALIDADE_MINUTOS)));
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            if (remetente != null && !remetente.isBlank()) mensagem.setFrom(remetente);
            mensagem.setTo(usuario.getEmail() == null || usuario.getEmail().isBlank() ? usuario.getUsername() : usuario.getEmail());
            mensagem.setSubject("Código para redefinir sua senha - Nazo Moçambique");
            mensagem.setText("Olá,\n\nSeu código de verificação para redefinir a senha é: " + codigo
                    + "\n\nEste código expira em " + VALIDADE_MINUTOS + " minutos e pode ser usado uma única vez."
                    + "\nSe você não solicitou esta alteração, ignore este e-mail.\n\nNazo Moçambique");
            mailSender.send(mensagem);
        } catch (MailException ex) {
            codigos.delete(registro);
            throw ex;
        }
    }


    @Transactional
    public boolean redefinir(String email, String codigo, String novaSenha, String confirmacao) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarios.findByUsername(normalizado).orElse(null);
        if (usuario == null) return false;
        CodigoRedefinicaoSenha registro = codigos.findByUsuario(usuario).orElse(null);
        if (registro == null || registro.isUtilizado() || registro.estaExpirado(LocalDateTime.now())
                || registro.getTentativas() >= MAX_TENTATIVAS) return false;
        if (codigo == null || !encoder.matches(codigo.trim(), registro.getCodigoHash())) {
            registro.incrementarTentativas();
            codigos.save(registro);
            return false;
        }
        if (novaSenha == null || novaSenha.length() < 8 || novaSenha.length() > 64
                || novaSenha.chars().noneMatch(Character::isLetter)
                || novaSenha.chars().noneMatch(Character::isDigit)
                || confirmacao == null || !novaSenha.equals(confirmacao)) return false;
        usuario.setSenha(encoder.encode(novaSenha));
        usuarios.save(usuario);
        registro.marcarUtilizado();
        codigos.save(registro);
        return true;
    }
}
