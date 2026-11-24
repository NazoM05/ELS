package com.example.nazomocambique.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name="movimento_carteira", indexes={@Index(name="idx_mov_carteira_usuario", columnList="usuario_id"), @Index(name="idx_mov_carteira_referencia", columnList="referencia", unique=true)})
public class MovimentoCarteira {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false, fetch=FetchType.LAZY) private Usuario usuario;
 @Column(nullable=false, precision=12, scale=2) private BigDecimal valor;
 @Column(nullable=false, length=20) private String tipo;
 @Column(length=120, unique=true) private String referencia;
 @Column(nullable=false, length=240) private String descricao;
 @Column(nullable=false) private LocalDateTime criadoEm=LocalDateTime.now();
 protected MovimentoCarteira() {}
 public MovimentoCarteira(Usuario u, BigDecimal v, String t, String r, String d){usuario=u;valor=v;tipo=t;referencia=r;descricao=d;}
 public Long getId(){return id;} public Usuario getUsuario(){return usuario;} public BigDecimal getValor(){return valor;} public String getTipo(){return tipo;} public String getReferencia(){return referencia;} public String getDescricao(){return descricao;} public LocalDateTime getCriadoEm(){return criadoEm;}
}
