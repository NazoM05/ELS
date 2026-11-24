package com.example.nazomocambique.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/** Configuração persistente da campanha BLACK. */
@Service
public class BlackFridayService {
    private final Path arquivo = Paths.get("uploads/config/black-friday.properties");
    private volatile boolean ativo;
    private volatile BigDecimal percentual = BigDecimal.ZERO;

    @PostConstruct
    void carregar() {
        if (!Files.isRegularFile(arquivo)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            p.load(r);
            ativo = Boolean.parseBoolean(p.getProperty("ativo", "false"));
            try {
                percentual = new BigDecimal(p.getProperty("percentual", "0"));
            } catch (NumberFormatException e) {
                percentual = BigDecimal.ZERO;
            }
            if (percentual.signum() < 0 || percentual.compareTo(new BigDecimal("100")) > 0) {
                percentual = BigDecimal.ZERO;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar a configuração da campanha BLACK.", e);
        }
    }

    public boolean isAtivo() {
        return ativo;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public BigDecimal aplicar(BigDecimal preco) {
        if (!ativo || preco == null || percentual.signum() <= 0) return preco;
        return preco.multiply(BigDecimal.valueOf(100).subtract(percentual))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    public synchronized void salvar(boolean novoEstado, BigDecimal novoPercentual) throws IOException {
        if (novoPercentual == null || novoPercentual.signum() < 0 || novoPercentual.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("O desconto BLACK deve estar entre 0% e 100%.");
        }
        Properties p = new Properties();
        p.setProperty("ativo", Boolean.toString(novoEstado));
        p.setProperty("percentual", novoPercentual.stripTrailingZeros().toPlainString());
        Files.createDirectories(arquivo.getParent());
        try (Writer w = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            p.store(w, "Configuração da campanha BLACK");
        }
        ativo = novoEstado;
        percentual = novoPercentual;
    }
}
