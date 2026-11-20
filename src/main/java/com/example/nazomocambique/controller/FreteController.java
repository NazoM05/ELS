package com.example.nazomocambique.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.nazomocambique.model.DadosEntrega;
import com.example.nazomocambique.model.OpcaoFrete;
import com.example.nazomocambique.model.ResumoCheckout;
import com.example.nazomocambique.service.PedidoService;

/**
 * Cotação de frete para a tela de finalização (exige login, como o resto do checkout).
 * O navegador manda CEP, estado e os mesmos "item" do carrinho; o servidor recalcula tudo e devolve
 * as opções de entrega. O valor que vale é sempre o recalculado ao confirmar o pedido.
 */
@RestController
public class FreteController {

	/** Resposta em JSON. "frete" é null enquanto não há opção (estado em branco). */
	public record Cotacao(BigDecimal subtotal, List<OpcaoFrete> opcoes, String escolhida, BigDecimal frete,
			BigDecimal total, BigDecimal faltaParaFreteGratis) {
	}

	private final PedidoService pedidoService;

	public FreteController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@GetMapping("/frete/cotar")
	public Cotacao cotar(@RequestParam(defaultValue = "") String cep, @RequestParam(defaultValue = "") String uf,
			@RequestParam(name = "servico", defaultValue = "") String servico,
			@RequestParam(name = "item", required = false) List<String> item) {
		DadosEntrega dados = new DadosEntrega();
		dados.setCep(cep);
		dados.setUf(uf);
		dados.setServicoFrete(servico);
		ResumoCheckout resumo = pedidoService.montarResumo(PedidoService.lerItens(item), dados);
		OpcaoFrete escolhida = resumo.getFreteEscolhido();
		return new Cotacao(resumo.getSubtotal(), resumo.getOpcoesFrete(), escolhida == null ? null : escolhida.codigo(),
				resumo.getFrete(), resumo.getTotal(), resumo.getFaltaParaFreteGratis());
	}
}
