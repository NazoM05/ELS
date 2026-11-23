package com.example.nazomocambique.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.stereotype.Service;

import com.example.nazomocambique.config.PixProperties;

import jakarta.annotation.PostConstruct;

/** Configuração editável do Pix estático, persistida fora do pacote da aplicação. */
@Service
public class PixConfiguracaoService {
    private final Path arquivo = Paths.get("uploads/config/pix.properties");
    private final PixProperties padrao;
    private volatile PixProperties atual;

    public PixConfiguracaoService(PixProperties padrao) {
        this.padrao = normalizar(padrao);
        this.atual = this.padrao;
    }

    @PostConstruct
    void carregar() {
        if (!Files.isRegularFile(arquivo)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            p.load(r);
            atual = normalizar(new PixProperties(p.getProperty("chave"), p.getProperty("nome"), p.getProperty("cidade")));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar a configuração do Pix.", e);
        }
    }

    /** A aba /admin/pix é a única fonte de verdade para os novos QR Codes. */
    public PixProperties obter() { return atual; }

    /** Compatibilidade com chamadas antigas; não usa fallback para evitar QR com dados divergentes da aba Pix. */
    public PixProperties obter(PixProperties ignorado) { return atual; }

    public PixProperties atual() { return atual; }

    private static PixProperties normalizar(PixProperties dados) {
        if (dados == null) return new PixProperties("", "", "");
        return new PixProperties(normalizarChave(dados.chave()), dados.nome(), dados.cidade());
    }

    /** Remove pontuação de CNPJ/CPF formatados, preservando outras chaves Pix (e-mail, telefone, EVP). */
    static String normalizarChave(String chave) {
        if (chave == null) return "";
        String limpa = chave.trim();
        if (limpa.matches("[0-9./-]+")) {
            String digitos = limpa.replaceAll("\\D", "");
            if (digitos.length() == 11 || digitos.length() == 14) return digitos;
        }
        return limpa;
    }

    public synchronized void salvar(String chave, String nome, String cidade) throws IOException {
        PixProperties nova = normalizar(new PixProperties(chave, nome, cidade));
        if (nova.chave().isBlank() || nova.nome().isBlank() || nova.cidade().isBlank())
            throw new IllegalArgumentException("Preencha a chave Pix, o nome do recebedor e a cidade.");
        if (nova.chave().length() > 77 || nova.nome().length() > 25 || nova.cidade().length() > 15)
            throw new IllegalArgumentException("Confira os limites: chave 77, nome 25 e cidade 15 caracteres.");
        if (!nova.chave().chars().allMatch(c -> c >= 0x20 && c <= 0x7E))
            throw new IllegalArgumentException("A chave Pix deve conter apenas caracteres ASCII imprimíveis.");
        Properties p = new Properties();
        p.setProperty("chave", nova.chave());
        p.setProperty("nome", nova.nome());
        p.setProperty("cidade", nova.cidade());
        Files.createDirectories(arquivo.getParent());
        try (Writer w = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            p.store(w, "Dados do recebedor do Pix estático");
        }
        atual = nova;
    }
}
