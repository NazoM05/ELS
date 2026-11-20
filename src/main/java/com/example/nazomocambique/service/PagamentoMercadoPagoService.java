package com.example.nazomocambique.service;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.nazomocambique.config.MercadoPagoProperties;
import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.service.GatewayPagamento.Pagamento;
import com.example.nazomocambique.service.GatewayPagamento.Pix;
import com.example.nazomocambique.service.GatewayPagamento.SolicitacaoPix;

/**
 * Pagamento por Pix pelo Mercado Pago: gera a cobrança do pedido e, quando o webhook avisa, confirma o
 * pagamento consultando a API (o aviso em si nunca é usado como prova) e passa o pedido para "Pagamento confirmado".
 * As chamadas HTTP ficam fora das transações do banco.
 */
@Service
public class PagamentoMercadoPagoService {

	private static final Logger log = LoggerFactory.getLogger(PagamentoMercadoPagoService.class);
	private static final String RESPONSAVEL = "Mercado Pago";
	private static final String APROVADO = "approved";

	private final MercadoPagoProperties props;
	private final GatewayPagamento gateway;
	private final PedidoRepository pedidoRepository;
	private final TransactionTemplate tx;

	public PagamentoMercadoPagoService(MercadoPagoProperties props, GatewayPagamento gateway,
			PedidoRepository pedidoRepository, PlatformTransactionManager transactionManager) {
		this.props = props;
		this.gateway = gateway;
		this.pedidoRepository = pedidoRepository;
		this.tx = new TransactionTemplate(transactionManager);
	}

	/** Mercado Pago ligado e com credenciais: só então o checkout tenta cobrar. */
	public boolean disponivel() {
		return props.configurado();
	}

	// ---------------------------------------------------------------- gerar o Pix

	/**
	 * Gera o Pix do pedido (só do dono, só se aguardando pagamento e escolhido Pix). Se já existe um Pix
	 * ainda válido, não cria outro.
	 */
	public void gerarPix(Long pedidoId, Long usuarioId) {
		if (!disponivel()) {
			throw new PagamentoIndisponivelException("O pagamento online não está disponível no momento.");
		}
		SolicitacaoPix solicitacao = tx.execute(status -> preparar(pedidoId, usuarioId));
		if (solicitacao == null) {
			return; // já tem um Pix válido
		}
		Pix pix = gateway.criarPix(solicitacao);
		tx.executeWithoutResult(status -> gravar(pedidoId, pix));
	}

	private SolicitacaoPix preparar(Long pedidoId, Long usuarioId) {
		Pedido pedido = pedidoRepository.findByIdAndUsuarioId(pedidoId, usuarioId)
				.orElseThrow(() -> new PagamentoIndisponivelException("Pedido não encontrado."));
		if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
			throw new PagamentoIndisponivelException("Este pedido não está aguardando pagamento.");
		}
		if (pedido.getFormaPagamento() != FormaPagamento.PIX) {
			throw new PagamentoIndisponivelException("Este pedido não foi feito com Pix.");
		}
		if (pedido.isPixAtivo()) {
			return null;
		}
		Usuario usuario = pedido.getUsuario();
		String nome = usuario.getNome() == null || usuario.getNome().isBlank() ? pedido.getNomeDestinatario() : usuario.getNome();
		return new SolicitacaoPix(pedido.getCodigo(), pedido.getTotal(), "Pedido " + pedido.getCodigo(),
				usuario.getUsername(), nome, usuario.getCpf(), props.pixExpiracaoMinutos(), UUID.randomUUID().toString());
	}

	private void gravar(Long pedidoId, Pix pix) {
		Pedido pedido = pedidoRepository.findById(pedidoId)
				.orElseThrow(() -> new PagamentoIndisponivelException("Pedido não encontrado."));
		pedido.setMpPaymentId(pix.paymentId());
		pedido.setMpStatus(pix.status());
		pedido.setPixCopiaECola(pix.copiaECola());
		pedido.setPixQrBase64(pix.qrBase64().isEmpty() ? null : pix.qrBase64());
		pedido.setPixExpiraEm(pix.expiraEm().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
		pedido.registrarAlteracao(pedido.getStatus(), pedido.getStatus(), false, null, null, RESPONSAVEL, null,
				"Pix gerado (pagamento #" + pix.paymentId() + ").");
		pedidoRepository.save(pedido);
	}

	// ---------------------------------------------------------------- webhook

	/**
	 * Chamado pelo webhook: consulta o pagamento na API do Mercado Pago e atualiza o pedido. Pode ser chamado
	 * várias vezes para o mesmo pagamento (o Mercado Pago reenvia avisos): aprovado uma vez, o resto é ignorado.
	 *
	 * @throws PagamentoIndisponivelException se a API não respondeu (o webhook devolve erro e o Mercado Pago tenta de novo)
	 */
	public void processarNotificacao(long paymentId) {
		Optional<Pagamento> pagamento = gateway.consultar(paymentId);
		if (pagamento.isEmpty()) {
			log.info("Notificação do Mercado Pago para o pagamento {}, que a API não conhece. Ignorada.", paymentId);
			return;
		}
		tx.executeWithoutResult(status -> aplicar(pagamento.get()));
	}

	private void aplicar(Pagamento pg) {
		if (pg.referencia() == null || pg.referencia().isBlank()) {
			log.info("Pagamento {} sem referência de pedido. Ignorado.", pg.id());
			return;
		}
		Optional<Pedido> encontrado = pedidoRepository.buscarParaAtualizar(CodigoPedido.normalizar(pg.referencia()));
		if (encontrado.isEmpty()) {
			log.warn("Pagamento {} aponta para o pedido {}, que não existe.", pg.id(), pg.referencia());
			return;
		}
		Pedido pedido = encontrado.get();

		if (!APROVADO.equals(pg.status())) {
			// Pagamento ainda pendente, recusado ou expirado: só guarda a situação do Pix que está em uso
			if (pedido.getMpPaymentId() != null && pedido.getMpPaymentId() == pg.id()) {
				pedido.setMpStatus(pg.status());
				pedidoRepository.save(pedido);
			}
			return;
		}
		if (pedido.getMpPaymentId() != null && pedido.getMpPaymentId() == pg.id() && APROVADO.equals(pedido.getMpStatus())) {
			return; // aviso repetido: já tratado
		}
		pedido.setMpPaymentId(pg.id());
		pedido.setMpStatus(APROVADO);

		StatusPedido atual = pedido.getStatus();
		if (pg.valor() == null || pg.valor().compareTo(pedido.getTotal()) != 0) {
			log.warn("Pagamento {} aprovado com valor {} diferente do pedido {} ({}).", pg.id(), pg.valor(),
					pedido.getCodigo(), pedido.getTotal());
			pedido.registrarAlteracao(atual, atual, false, null, null, RESPONSAVEL, null,
					"Pagamento #" + pg.id() + " aprovado com valor diferente do pedido. Conferir no Mercado Pago antes de seguir.");
		} else if (atual == StatusPedido.AGUARDANDO_PAGAMENTO) {
			pedido.setStatus(StatusPedido.PAGO);
			pedido.registrarAlteracao(atual, StatusPedido.PAGO, false, null, null, RESPONSAVEL, null,
					"Pagamento aprovado (Mercado Pago #" + pg.id() + ").");
		} else if (atual == StatusPedido.CANCELADO) {
			// Não reabre o pedido (o estoque já voltou): avisa a equipe para devolver o dinheiro
			pedido.registrarAlteracao(atual, atual, false, null, null, RESPONSAVEL, null,
					"Pagamento #" + pg.id() + " aprovado depois do cancelamento. Devolver o valor ao cliente.");
		}
		pedidoRepository.save(pedido);
	}
}
