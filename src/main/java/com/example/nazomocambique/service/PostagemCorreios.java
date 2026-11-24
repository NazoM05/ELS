package com.example.nazomocambique.service;

import com.example.nazomocambique.model.PrePostagem;
import com.example.nazomocambique.model.SolicitacaoPostagem;

/** Fonte de pré-postagens dos Correios (a implementação real é o CorreiosClient; nos testes entra uma falsa). */
public interface PostagemCorreios {

	/**
	 * Cria a pré-postagem e devolve o código de rastreamento gerado pelos Correios.
	 *
	 * @throws CorreiosIndisponivelException se a API não respondeu ou recusou a pré-postagem
	 */
	PrePostagem criarPrePostagem(SolicitacaoPostagem solicitacao);
}
