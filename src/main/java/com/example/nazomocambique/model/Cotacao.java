package com.example.nazomocambique.model;

import java.math.BigDecimal;

/** Resposta dos Correios para um serviço: preço e prazo (dias úteis). */
public record Cotacao(BigDecimal valor, int prazoDias) {
}
