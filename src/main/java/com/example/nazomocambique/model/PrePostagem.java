package com.example.nazomocambique.model;

/**
 * Resposta dos Correios a uma pré-postagem.
 *
 * @param idPrePostagem identificador da pré-postagem no sistema dos Correios
 * @param codigoObjeto  código de rastreamento gerado pelos Correios (ex.: AA123456789BR)
 */
public record PrePostagem(String idPrePostagem, String codigoObjeto) {
}
