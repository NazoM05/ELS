package com.example.nazomocambique.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * Uma linha do acompanhamento do pedido (trilha de auditoria): quem mexeu (nome e login), quando,
 * de qual etapa para qual, o rastreio anterior e o novo, e a observação.
 * As colunas não são atualizáveis: uma vez gravada, a linha não muda mais.
 */
@Entity
@Getter
@Setter
public class HistoricoPedido {

	private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final DateTimeFormatter FORMATO_COMPLETO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pedido_id", nullable = false, updatable = false)
	private Pedido pedido;

	/** Etapa antes da alteração; null na criação do pedido. */
	@Enumerated(EnumType.STRING)
	@Column(length = 30, updatable = false)
	private StatusPedido statusAnterior;

	/** Etapa depois da alteração. */
	@Enumerated(EnumType.STRING)
	@Column(length = 30, nullable = false, updatable = false)
	private StatusPedido status;

	/** Indica se o código de rastreio mudou nesta alteração (os dois valores abaixo valem só nesse caso). */
	@Column(nullable = false, updatable = false)
	private boolean alterouRastreio;

	@Column(length = 40, updatable = false)
	private String rastreioAnterior;

	@Column(length = 40, updatable = false)
	private String rastreioNovo;

	/** Nome mostrado (nome da conta ou "Cliente"). */
	@Column(length = 80, updatable = false)
	private String responsavel;

	/** Login (e-mail) de quem fez a alteração; o nome pode mudar, o login identifica a conta. */
	@Column(length = 120, updatable = false)
	private String usuarioLogin;

	@Column(length = 300, updatable = false)
	private String observacao;

	@Column(updatable = false)
	private LocalDateTime criadoEm;

	@PrePersist
	void aoCriar() {
		if (criadoEm == null) {
			criadoEm = LocalDateTime.now();
		}
	}

	@Transient
	public String getCriadoEmFormatado() {
		return criadoEm == null ? "" : criadoEm.format(FORMATO);
	}

	/** Com segundos: usado na tela de histórico, onde a ordem exata importa. */
	@Transient
	public String getCriadoEmCompleto() {
		return criadoEm == null ? "" : criadoEm.format(FORMATO_COMPLETO);
	}

	/** Esta linha trocou a etapa do pedido (de uma etapa para outra diferente). */
	@Transient
	public boolean isMudouEtapa() {
		return statusAnterior != null && statusAnterior != status;
	}

	/** Primeira linha do pedido (feito na loja). */
	@Transient
	public boolean isCriacao() {
		return statusAnterior == null;
	}
}
