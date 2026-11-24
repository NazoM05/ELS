package com.example.nazomocambique.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;

@Entity
public class CodigoRedefinicaoSenha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;


    @Column(nullable = false, length = 100)
    private String codigoHash;
    @Column(nullable = false)
    private LocalDateTime criadoEm;
    @Column(nullable = false)
    private LocalDateTime expiraEm;
    @Column(nullable = false)
    private int tentativas;
    @Column(nullable = false)
    private boolean utilizado;


    protected CodigoRedefinicaoSenha() {}

    public CodigoRedefinicaoSenha(Usuario usuario, String codigoHash, LocalDateTime criadoEm, LocalDateTime expiraEm) {
        this.usuario = usuario;
        this.codigoHash = codigoHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
        this.tentativas = 0;
        this.utilizado = false;
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getCodigoHash() { return codigoHash; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public int getTentativas() { return tentativas; }
    public boolean isUtilizado() { return utilizado; }
    public void incrementarTentativas() { tentativas++; }
    public void marcarUtilizado() { utilizado = true; }
    public boolean estaExpirado(LocalDateTime agora) { return agora.isAfter(expiraEm); }
}
