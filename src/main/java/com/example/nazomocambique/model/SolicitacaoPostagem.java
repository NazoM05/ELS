package com.example.nazomocambique.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que a loja envia aos Correios para pré-postar um pedido (a API devolve o código do objeto).
 * Só dados reais do pedido: nada aqui é inventado.
 *
 * @param codigoServico código do serviço contratado (ex.: 03298 = PAC, 03220 = SEDEX)
 */
public record SolicitacaoPostagem(String codigoServico, Destino destino, Pacote pacote, List<Item> itens) {

	/** Endereço do destinatário, já separado em campos (telefone em DDD + número, só dígitos). */
	public record Destino(String nome, String ddd, String telefone, String cep, String logradouro, String numero,
			String complemento, String bairro, String cidade, String uf) {
	}

	/** Linha da declaração de conteúdo. */
	public record Item(String conteudo, int quantidade, BigDecimal valor) {
	}
}
