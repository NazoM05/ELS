package com.example.nazomocambique.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/** Uma linha do pedido: produto + tamanho + quantidade, com o preço do momento da compra. */
@Entity
@Getter
@Setter
public class ItemPedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pedido_id", nullable = false)
	private Pedido pedido;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produto_id")
	private Produto produto;

	@Column(nullable = false)
	private String nomeProduto;

	@Column(nullable = false, length = 20)
	private String tamanho;

	private int quantidade;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal precoUnitario;

	private String imagem; // nome do arquivo em /img/{nome}, copiado do produto

	@Transient
	public BigDecimal getTotal() {
		return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
	}
}
