package com.example.nazomocambique.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name="configuracao_cashback")
public class ConfiguracaoCashback {
 @Id private Long id=1L;
 @Column(nullable=false) private boolean ativo=false;
 @Column(nullable=false, precision=7, scale=3) private BigDecimal percentual=new BigDecimal("5.000");
 @Column(nullable=false, precision=12, scale=2) private BigDecimal minimoCompra=BigDecimal.ZERO;
 @Column(nullable=false, precision=12, scale=2) private BigDecimal limitePorCompra=new BigDecimal("500.00");
 public ConfiguracaoCashback() {}
 public Long getId(){return id;} public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;} public BigDecimal getPercentual(){return percentual;} public void setPercentual(BigDecimal v){percentual=v;} public BigDecimal getMinimoCompra(){return minimoCompra;} public void setMinimoCompra(BigDecimal v){minimoCompra=v;} public BigDecimal getLimitePorCompra(){return limitePorCompra;} public void setLimitePorCompra(BigDecimal v){limitePorCompra=v;}
}
