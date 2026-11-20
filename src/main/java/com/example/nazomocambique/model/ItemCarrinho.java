package com.example.nazomocambique.model;

/** Item vindo do carrinho do navegador: produto, tamanho escolhido e quantidade. */
public record ItemCarrinho(Long produtoId, String tamanho, int quantidade) {
}
