package com.example.nazomocambique.model;

import java.math.BigDecimal;

/**
 * Uma forma de entrega oferecida ao cliente.
 *
 * @param codigo     código do serviço dos Correios (ex.: 03298) ou "PADRAO" para o frete fixo da loja
 * @param origem     "CORREIOS" (cotação da API) ou "LOJA" (regra fixa por estado)
 * @param prazoDias  dias úteis para entregar
 * @param gratis     true quando a regra de frete grátis da loja cobriu o valor
 */
public record OpcaoFrete(String codigo, String nome, BigDecimal valor, int prazoDias, String origem, boolean gratis) {

	public OpcaoFrete comValorGratis() {
		return new OpcaoFrete(codigo, nome, BigDecimal.ZERO.setScale(2), prazoDias, origem, true);
	}
}
