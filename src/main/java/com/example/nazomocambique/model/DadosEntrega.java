package com.example.nazomocambique.model;

import lombok.Getter;
import lombok.Setter;

/** Campos do formulário de entrega e pagamento da tela de finalização da compra. */
@Getter
@Setter
public class DadosEntrega {
	private String nome = "";
	private String telefone = "";
	private String cep = "";
	private String endereco = "";
	private String numero = "";
	private String complemento = "";
	private String bairro = "";
	private String cidade = "";
	private String uf = "";
	private String formaPagamento = "";
	/** Código da opção de frete escolhida (ex.: 03298 = PAC); vazio = a mais barata. */
	private String servicoFrete = "";
}
