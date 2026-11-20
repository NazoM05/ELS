package com.example.nazomocambique.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * Pedido gerado na finalização da compra (/checkout). Guarda uma cópia do nome e do preço de cada
 * item no momento da compra, então editar o produto depois não altera pedidos antigos.
 */
@Entity
@Getter
@Setter
public class Pedido {

	private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Código de acompanhamento mostrado ao cliente e usado pela equipe (ex.: EL-261120-7K3Q). */
	@Column(nullable = false, unique = true, updatable = false, length = 20)
	private String codigo;

	@Enumerated(EnumType.STRING)
	@Column(length = 30, nullable = false)
	private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

	/** Código de rastreio dos Correios/transportadora, preenchido pela equipe no envio. */
	@Column(length = 40)
	private String codigoRastreio;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	// Entrega
	@Column(nullable = false, length = 80)
	private String nomeDestinatario;

	@Column(length = 11)
	private String telefone; // só dígitos (DDD + número)

	@Column(length = 8)
	private String cep; // só dígitos

	@Column(length = 150)
	private String endereco;

	@Column(length = 20)
	private String numero;

	@Column(length = 60)
	private String complemento;

	@Column(length = 80)
	private String bairro;

	@Column(length = 80)
	private String cidade;

	@Column(length = 2)
	private String uf;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private FormaPagamento formaPagamento;

	// Valores calculados no servidor a partir do preço atual de cada produto
	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal subtotal;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal frete;

	// Forma de entrega escolhida (ex.: "PAC") e prazo em dias úteis, copiados no momento da compra
	@Column(length = 60)
	private String servicoFrete;

	private Integer prazoFreteDias;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal total;

	private LocalDateTime criadoEm;

	private LocalDateTime atualizadoEm;

	// Pagamento pelo Mercado Pago (Pix): id e status do pagamento no gateway e o Pix em uso
	private Long mpPaymentId;

	@Column(length = 30)
	private String mpStatus;

	/** Código "copia e cola" do Pix. */
	@Column(length = 1024)
	private String pixCopiaECola;

	/** Imagem PNG do QR Code em base64. */
	@Lob
	private String pixQrBase64;

	private LocalDateTime pixExpiraEm;

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("id")
	private List<ItemPedido> itens = new ArrayList<>();

	// LAZY: "itens" já é EAGER e o Hibernate não carrega duas listas EAGER juntas
	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<HistoricoPedido> historico = new ArrayList<>();

	@PrePersist
	void aoCriar() {
		this.criadoEm = LocalDateTime.now();
		this.atualizadoEm = this.criadoEm;
	}

	/** Linha de criação do pedido (não há etapa anterior). */
	public void registrar(StatusPedido status, String responsavel, String observacao) {
		registrarAlteracao(null, status, false, null, null, responsavel, null, observacao);
	}

	/**
	 * Grava uma linha no histórico com os valores anterior e novo da etapa (e do rastreio, quando mudou),
	 * quem fez (nome e login) e quando.
	 */
	public void registrarAlteracao(StatusPedido statusAnterior, StatusPedido statusNovo, boolean alterouRastreio,
			String rastreioAnterior, String rastreioNovo, String responsavel, String usuarioLogin, String observacao) {
		HistoricoPedido h = new HistoricoPedido();
		h.setPedido(this);
		h.setStatusAnterior(statusAnterior);
		h.setStatus(statusNovo);
		h.setAlterouRastreio(alterouRastreio);
		h.setRastreioAnterior(alterouRastreio ? rastreioAnterior : null);
		h.setRastreioNovo(alterouRastreio ? rastreioNovo : null);
		h.setResponsavel(responsavel);
		h.setUsuarioLogin(usuarioLogin);
		h.setObservacao(observacao);
		h.setCriadoEm(LocalDateTime.now());
		historico.add(h);
		this.atualizadoEm = h.getCriadoEm();
	}

	public void adicionarItem(ItemPedido item) {
		item.setPedido(this);
		itens.add(item);
	}

	@Transient
	public int getQuantidadeTotal() {
		return itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
	}

	@Transient
	public String getCriadoEmFormatado() {
		return criadoEm == null ? null : criadoEm.format(FORMATO_DATA);
	}

	/** Ex.: "Rua das Flores, 123 - Apto 4 - Centro - Porto Alegre/RS - CEP 90000-000" */
	@Transient
	public String getEnderecoCompleto() {
		StringBuilder sb = new StringBuilder();
		sb.append(endereco).append(", ").append(numero);
		if (complemento != null && !complemento.isBlank()) {
			sb.append(" - ").append(complemento);
		}
		sb.append(" - ").append(bairro).append(" - ").append(cidade).append('/').append(uf);
		if (cep != null && cep.length() == 8) {
			sb.append(" - CEP ").append(cep, 0, 5).append('-').append(cep.substring(5));
		}
		return sb.toString();
	}

	/** Histórico do mais novo para o mais antigo (como a equipe lê). */
	@Transient
	public List<HistoricoPedido> getHistoricoRecente() {
		List<HistoricoPedido> copia = new ArrayList<>(historico);
		java.util.Collections.reverse(copia);
		return copia;
	}

	@Transient
	public String getAtualizadoEmFormatado() {
		return atualizadoEm == null ? null : atualizadoEm.format(FORMATO_DATA);
	}

	/** Telefone com máscara: (51) 99999-9999 ou (51) 3333-4444. */
	@Transient
	public String getTelefoneFormatado() {
		if (telefone == null || telefone.length() < 10) {
			return telefone;
		}
		int corte = telefone.length() == 11 ? 7 : 6;
		return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, corte) + "-" + telefone.substring(corte);
	}

	/** Pedido feito com Pix que ainda espera o pagamento. */
	@Transient
	public boolean isAguardandoPix() {
		return status == StatusPedido.AGUARDANDO_PAGAMENTO && formaPagamento == FormaPagamento.PIX;
	}

	/** Existe um Pix gerado que ainda não venceu. */
	@Transient
	public boolean isPixAtivo() {
		return isAguardandoPix() && pixCopiaECola != null && pixExpiraEm != null && pixExpiraEm.isAfter(LocalDateTime.now());
	}

	@Transient
	public String getPixExpiraEmFormatado() {
		return pixExpiraEm == null ? null : pixExpiraEm.format(FORMATO_DATA);
	}

	@Transient
	public boolean isFreteGratis() {
		return frete != null && frete.signum() == 0;
	}
}
