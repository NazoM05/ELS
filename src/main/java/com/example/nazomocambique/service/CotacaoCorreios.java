package com.example.nazomocambique.service;

import com.example.nazomocambique.model.Cotacao;
import com.example.nazomocambique.model.Pacote;

/** Fonte de cotações dos Correios (a implementação real é o CorreiosClient; nos testes entra uma falsa). */
public interface CotacaoCorreios {

	/**
	 * @throws CorreiosIndisponivelException se a API não respondeu ou recusou a consulta
	 */
	Cotacao cotar(String codigoServico, String cepDestino, Pacote pacote);
}
