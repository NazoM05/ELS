package com.example.nazomocambique.model;

/** Volume que será enviado, no formato que a API de preço dos Correios pede (gramas e centímetros). */
public record Pacote(int pesoGramas, int comprimentoCm, int larguraCm, int alturaCm) {
}
