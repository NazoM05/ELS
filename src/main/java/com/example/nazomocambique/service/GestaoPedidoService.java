package com.example.nazomocambique.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.ItemPedido;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.repository.ProdutoRepository;

/**
 * Aba "Pedidos" do administrador: lista, busca pelo código e acompanhamento (etapa, rastreio e anotações).
 * Cancelar devolve ao estoque o que o pedido tinha baixado; depois disso o pedido não muda mais de etapa.
 */
@Service
public class GestaoPedidoService {

	public static final int MAX_OBSERVACAO = 300;
	public static final int MAX_RASTREIO = 40;

	private final PedidoRepository pedidoRepository;
	private final ProdutoRepository produtoRepository;

	public GestaoPedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
		this.pedidoRepository = pedidoRepository;
		this.produtoRepository = produtoRepository;
	}

	/** Mais recentes primeiro, com o cliente já carregado para a tabela. */
	@Transactional(readOnly = true)
	public List<Pedido> listarTodos() {
		List<Pedido> pedidos = pedidoRepository.findAllByOrderByCriadoEmDescIdDesc();
		pedidos.forEach(p -> p.getUsuario().getUsername());
		return pedidos;
	}

	public static Map<StatusPedido, Long> contarPorStatus(List<Pedido> pedidos) {
		Map<StatusPedido, Long> contagem = new EnumMap<>(StatusPedido.class);
		for (StatusPedido s : StatusPedido.values()) {
			contagem.put(s, 0L);
		}
		pedidos.forEach(p -> contagem.merge(p.getStatus(), 1L, Long::sum));
		return contagem;
	}

	/** Aceita o código com minúsculas, espaços ou sem hífens. */
	@Transactional(readOnly = true)
	public Optional<Pedido> buscarPorCodigo(String codigo) {
		Optional<Pedido> pedido = pedidoRepository.findByCodigo(CodigoPedido.normalizar(codigo));
		pedido.ifPresent(p -> {
			p.getUsuario().getUsername();
			p.getHistorico().size();
		});
		return pedido;
	}

	/**
	 * Muda a etapa, o código de rastreio e/ou anota uma observação. Cada mudança vira uma linha
	 * no histórico com o nome de quem fez. Sem nenhuma mudança, nada é gravado.
	 *
	 * @return true se algo mudou
	 */
	@Transactional
	public boolean atualizar(String codigo, StatusPedido novoStatus, String rastreio, String observacao, String responsavel) {
		return atualizar(codigo, novoStatus, rastreio, observacao, responsavel, null);
	}

	/**
	 * Igual ao anterior, guardando também o login de quem fez a alteração (usado pelas telas do admin e da
	 * separação). O histórico registra a etapa anterior e a nova, e o rastreio anterior e o novo quando mudou.
	 */
	@Transactional
	public boolean atualizar(String codigo, StatusPedido novoStatus, String rastreio, String observacao, String responsavel,
			String login) {
		Pedido pedido = pedidoRepository.findByCodigo(CodigoPedido.normalizar(codigo))
				.orElseThrow(() -> new AtualizacaoPedidoException("Pedido não encontrado."));
		if (novoStatus == null) {
			throw new AtualizacaoPedidoException("Escolha a etapa do pedido.");
		}
		String obs = observacao == null ? "" : observacao.trim();
		if (obs.length() > MAX_OBSERVACAO) {
			throw new AtualizacaoPedidoException("A observação pode ter até " + MAX_OBSERVACAO + " caracteres.");
		}
		String rastreioNovo = rastreio == null ? "" : rastreio.replaceAll("\\s", "").toUpperCase();
		if (rastreioNovo.length() > MAX_RASTREIO || !rastreioNovo.matches("[A-Z0-9-]*")) {
			throw new AtualizacaoPedidoException("O código de rastreio deve ter só letras e números (até " + MAX_RASTREIO + ").");
		}
		StatusPedido atual = pedido.getStatus();
		if (atual == StatusPedido.CANCELADO && novoStatus != StatusPedido.CANCELADO) {
			throw new AtualizacaoPedidoException("Pedido cancelado não pode voltar para outra etapa (o estoque já foi devolvido).");
		}

		boolean mudouStatus = novoStatus != atual;
		String rastreioAtual = pedido.getCodigoRastreio() == null ? "" : pedido.getCodigoRastreio();
		boolean mudouRastreio = !rastreioNovo.equals(rastreioAtual);
		if (!mudouStatus && !mudouRastreio && obs.isEmpty()) {
			return false;
		}

		if (mudouStatus && novoStatus == StatusPedido.CANCELADO) {
			devolverEstoque(pedido);
		}
		pedido.setStatus(novoStatus);
		pedido.setCodigoRastreio(rastreioNovo.isEmpty() ? null : rastreioNovo);

		StringBuilder texto = new StringBuilder();
		if (mudouRastreio) {
			texto.append(rastreioNovo.isEmpty() ? "Código de rastreio removido." : "Rastreio: " + rastreioNovo + ".");
		}
		if (mudouStatus && novoStatus == StatusPedido.CANCELADO) {
			texto.append(texto.isEmpty() ? "" : " ").append("Estoque devolvido.");
		}
		if (!obs.isEmpty()) {
			texto.append(texto.isEmpty() ? "" : " ").append(obs);
		}
		String quem = responsavel == null || responsavel.isBlank() ? "Equipe" : responsavel.trim();
		String loginGravado = login == null || login.isBlank() ? null : (login.length() > 120 ? login.substring(0, 120) : login);
		pedido.registrarAlteracao(atual, novoStatus, mudouRastreio, rastreioAtual.isEmpty() ? null : rastreioAtual,
				rastreioNovo.isEmpty() ? null : rastreioNovo, quem.length() > 80 ? quem.substring(0, 80) : quem, loginGravado,
				texto.isEmpty() ? null : texto.toString());
		pedidoRepository.save(pedido);
		return true;
	}

	private void devolverEstoque(Pedido pedido) {
		for (ItemPedido item : pedido.getItens()) {
			if (item.getProduto() == null) {
				continue; // produto apagado da loja: não há onde devolver
			}
			Produto produto = produtoRepository.buscarParaAtualizar(item.getProduto().getId()).orElse(null);
			if (produto == null) {
				continue;
			}
			String tamanho = Objects.requireNonNullElse(produto.resolverTamanho(item.getTamanho()), item.getTamanho());
			produto.getEstoque().put(tamanho, produto.estoqueDoTamanho(tamanho) + item.getQuantidade());
			produto.setVendidas(Math.max(produto.getVendidasTotal() - item.getQuantidade(), 0));
			produtoRepository.save(produto);
		}
	}

	/** Erro de validação mostrado na tela do pedido. */
	public static class AtualizacaoPedidoException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public AtualizacaoPedidoException(String mensagem) {
			super(mensagem);
		}
	}
}
