package com.example.nazomocambique.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo do carrinho conferido no servidor: preço atual de cada produto, tamanho existente
 * e estoque disponível. "temErros" = pelo menos uma linha não pode ser vendida como está.
 */
public class ResumoCheckout {

	private final List<Linha> linhas;
	private final BigDecimal subtotal;
	private final BigDecimal frete;
	private final BigDecimal total;
	private final BigDecimal faltaParaFreteGratis;
	private final List<OpcaoFrete> opcoesFrete;
	private final OpcaoFrete freteEscolhido;

	/** frete null = estado ainda não informado; faltaParaFreteGratis null = já tem frete grátis (ou sem estado). */
	public ResumoCheckout(List<Linha> linhas, BigDecimal subtotal, BigDecimal frete, BigDecimal faltaParaFreteGratis) {
		this(linhas, subtotal, frete, faltaParaFreteGratis, List.of(), null);
	}

	/** opcoesFrete = formas de entrega cotadas (Correios ou regra da loja); freteEscolhido = a que vale no total. */
	public ResumoCheckout(List<Linha> linhas, BigDecimal subtotal, BigDecimal frete, BigDecimal faltaParaFreteGratis,
			List<OpcaoFrete> opcoesFrete, OpcaoFrete freteEscolhido) {
		this.linhas = linhas;
		this.subtotal = subtotal;
		this.frete = frete;
		this.total = frete == null ? subtotal : subtotal.add(frete);
		this.faltaParaFreteGratis = faltaParaFreteGratis;
		this.opcoesFrete = opcoesFrete;
		this.freteEscolhido = freteEscolhido;
	}

	public List<Linha> getLinhas() { return linhas; }
	public BigDecimal getSubtotal() { return subtotal; }
	public BigDecimal getFrete() { return frete; }
	public BigDecimal getTotal() { return total; }
	public BigDecimal getFaltaParaFreteGratis() { return faltaParaFreteGratis; }
	public List<OpcaoFrete> getOpcoesFrete() { return opcoesFrete; }
	public OpcaoFrete getFreteEscolhido() { return freteEscolhido; }

	public boolean isVazio() { return linhas.isEmpty(); }
	public boolean isTemErros() { return linhas.stream().anyMatch(l -> l.getErro() != null); }
	public boolean isFreteGratis() { return frete != null && frete.signum() == 0; }
	public boolean isFreteDefinido() { return frete != null; }
	/** Unidades que entram no subtotal (linhas com problema ficam de fora). */
	public int getQuantidadeTotal() { return linhas.stream().filter(l -> l.getErro() == null).mapToInt(Linha::getQuantidade).sum(); }

	/** Uma linha do resumo. "parametro" é o valor enviado de volta no formulário (id:tamanho:quantidade). */
	public static class Linha {
		private final Long produtoId;
		private final String nome;
		private final String imagem;
		private final String tamanho;
		private final int quantidade;
		private final BigDecimal precoUnitario;
		private final Integer disponivel;
		private final String erro;

		public Linha(Long produtoId, String nome, String imagem, String tamanho, int quantidade,
				BigDecimal precoUnitario, Integer disponivel, String erro) {
			this.produtoId = produtoId;
			this.nome = nome;
			this.imagem = imagem;
			this.tamanho = tamanho;
			this.quantidade = quantidade;
			this.precoUnitario = precoUnitario;
			this.disponivel = disponivel;
			this.erro = erro;
		}

		public Long getProdutoId() { return produtoId; }
		public String getNome() { return nome; }
		public String getImagem() { return imagem; }
		public String getTamanho() { return tamanho; }
		public int getQuantidade() { return quantidade; }
		public BigDecimal getPrecoUnitario() { return precoUnitario; }
		public Integer getDisponivel() { return disponivel; }
		public String getErro() { return erro; }

		public BigDecimal getTotal() {
			return precoUnitario == null ? BigDecimal.ZERO : precoUnitario.multiply(BigDecimal.valueOf(quantidade));
		}

		public String getParametro() {
			return produtoId + ":" + tamanho + ":" + quantidade;
		}
	}
}
