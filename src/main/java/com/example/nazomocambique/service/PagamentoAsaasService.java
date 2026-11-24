package com.example.nazomocambique.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.nazomocambique.config.AsaasProperties;
import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.service.GatewayPagamento.Pagamento;
import com.example.nazomocambique.service.GatewayPagamento.Pix;
import com.example.nazomocambique.service.GatewayPagamento.SolicitacaoPix;

/**
 * Pagamento por Pix pelo Asaas: gera a cobrança do pedido e, quando o webhook avisa, confirma o
 * pagamento consultando a API (o aviso em si nunca é usado como prova) e passa o pedido para "Pagamento confirmado".
 * As chamadas HTTP ficam fora das transações do banco.
 */
@Service
public class PagamentoAsaasService {

	private static final Logger log = LoggerFactory.getLogger(PagamentoAsaasService.class);
	private static final String RESPONSAVEL = "Asaas";
	private static final String APROVADO = "RECEIVED";
	/** Intervalo mínimo entre duas consultas ao Asaas para o mesmo pedido (evita estourar o limite de requisições). */
	private static final long INTERVALO_MIN_CONSULTA_MS = 15_000;
	private static final int MAX_PEDIDOS_NO_CONTROLE = 2_000;

	private final AsaasProperties props;
	private final GatewayPagamento gateway;
	private final PedidoRepository pedidoRepository;
	private final TransactionTemplate tx;
	private final Map<Long, Long> ultimaConsulta = new ConcurrentHashMap<>();
    private final Environment environment;
	@Autowired(required = false) private CarteiraService carteiraService;

	public PagamentoAsaasService(AsaasProperties props, @Qualifier("asaasClient") GatewayPagamento gateway,
			PedidoRepository pedidoRepository, PlatformTransactionManager transactionManager, Environment environment) {
		this.props = props;
		this.gateway = gateway;
		this.pedidoRepository = pedidoRepository;
		this.tx = new TransactionTemplate(transactionManager);
		this.environment = environment;
	}

	/** Asaas ligado e com credenciais: só então o checkout tenta cobrar. */
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
		// Compatibilidade: contas antigas podem ainda não ter a coluna email preenchida.
		String email = usuario.getEmail();
		if (email == null || email.isBlank()) {
			String username = usuario.getUsername();
			if (username != null && username.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")) {
				email = username;
			}
		}
		// O e-mail é opcional no Asaas (o AsaasClient só o envia se existir): sem e-mail válido, o Pix é gerado mesmo assim.
		if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")) {
			email = null;
		} else {
			email = email.trim();
		}
		
		if (Arrays.asList(environment.getActiveProfiles()).contains("dev") && email != null) {
			email = "artoiszap@gmail.com";
		}
		
		return new SolicitacaoPix(pedido.getCodigo(), pedido.getTotal(), "Pedido " + pedido.getCodigo(),
				email, nome, usuario.getCpf(), props.pixExpiracaoMinutos(), UUID.randomUUID().toString());
	}

	private void gravar(Long pedidoId, Pix pix) {
		Pedido pedido = pedidoRepository.findById(pedidoId)
				.orElseThrow(() -> new PagamentoIndisponivelException("Pedido não encontrado."));
		pedido.setAsaasPaymentId(pix.paymentId());
		pedido.setAsaasStatus(pix.status());
		pedido.setPixCopiaECola(pix.copiaECola());
		// A imagem não é guardada: o QR Code é gerado na hora, a partir do Pix Copia e Cola (evita coluna pequena/CLOB e falha do Asaas sem imagem).
		pedido.setPixQrBase64(null);
		pedido.setPixExpiraEm(pix.expiraEm().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
		pedido.registrarAlteracao(pedido.getStatus(), pedido.getStatus(), false, null, null, RESPONSAVEL, null,
				"Pix gerado (pagamento #" + pix.paymentId() + ").");
		pedidoRepository.save(pedido);
	}

	/** Solicita reembolso integral ao Asaas para um pedido já cancelado. */
	public void estornarPedido(Long pedidoId) {
		String paymentId = tx.execute(status -> pedidoRepository.findById(pedidoId)
				.filter(p -> p.getStatus() == StatusPedido.CANCELADO)
				.map(Pedido::getAsaasPaymentId).orElse(null));
		if (paymentId != null) {
			Optional<Pagamento> pagamento = gateway.consultar(paymentId);
			if (pagamento.isPresent() && (APROVADO.equals(pagamento.get().status()) || "CONFIRMED".equals(pagamento.get().status()))) gateway.estornar(paymentId);
		}
	}

	// ---------------------------------------------------------------- sincronização (plano B do webhook)

	/**
	 * Confere no Asaas o pagamento de um pedido que ainda aguarda Pix e atualiza o pedido se foi pago. Serve de rede
	 * de segurança quando o webhook não chega (túnel fora do ar, falha de rede). Só consulta pedidos do dono, em
	 * "Aguardando pagamento", com cobrança já criada, no máximo uma vez a cada 15 s por pedido. Nunca lança
	 * exceção: se o Asaas não responder, o pedido segue com o status que já tem no banco.
	 *
	 * @return true se consultou o Asaas nesta chamada
	 */
	public boolean sincronizarPedido(Long pedidoId, Long usuarioId) {
		if (!disponivel()) {
			return false;
		}
		String paymentId = tx.execute(status -> pedidoRepository.findByIdAndUsuarioId(pedidoId, usuarioId)
				.filter(p -> p.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO)
				.map(Pedido::getAsaasPaymentId).orElse(null));
		if (paymentId == null || paymentId.isBlank() || !liberarConsulta(pedidoId)) {
			return false;
		}
		return consultarSemFalhar(paymentId);
	}

	/**
	 * Sincroniza os pedidos Pix pendentes criados nas últimas 24 h (usado pela tarefa agendada). Para na primeira
	 * falha de comunicação, para não esperar o tempo limite de cada pedido quando o Asaas está fora do ar.
	 *
	 * @return quantos pedidos foram consultados
	 */
	public int sincronizarPendentes() {
		if (!disponivel()) {
			return 0;
		}
		List<String> ids = tx.execute(status -> pedidoRepository
				.findAllByStatusAndFormaPagamentoAndAsaasPaymentIdIsNotNullAndCriadoEmAfter(
						StatusPedido.AGUARDANDO_PAGAMENTO, FormaPagamento.PIX, LocalDateTime.now().minusHours(24))
				.stream().map(Pedido::getAsaasPaymentId).toList());
		int consultados = 0;
		for (String paymentId : ids == null ? List.<String>of() : ids) {
			if (!consultarSemFalhar(paymentId)) {
				break;
			}
			consultados++;
		}
		return consultados;
	}

	/**
	 * Rede de segurança do webhook: a cada 2 minutos confere no Asaas os pedidos Pix ainda pendentes, para que um
	 * aviso perdido (túnel fora do ar, falha de rede) não deixe um pedido pago como "Aguardando pagamento".
	 * O intervalo pode ser trocado com a propriedade asaas.sincronizacao-intervalo-ms.
	 */
	@Scheduled(initialDelay = 60_000, fixedDelayString = "${asaas.sincronizacao-intervalo-ms:120000}")
	public void sincronizarPeriodicamente() {
		sincronizarPendentes();
	}

	private boolean consultarSemFalhar(String paymentId) {
		try {
			processarNotificacao(paymentId);
			return true;
		} catch (RuntimeException e) {
			log.warn("Não foi possível sincronizar o pagamento {} com o Asaas: {}", paymentId, e.getMessage());
			return false;
		}
	}

	/** Controle simples por pedido; o mapa é limpo se crescer demais (pedidos antigos nunca mais são consultados). */
	private boolean liberarConsulta(Long pedidoId) {
		long agora = System.currentTimeMillis();
		if (ultimaConsulta.size() > MAX_PEDIDOS_NO_CONTROLE) {
			ultimaConsulta.clear();
		}
		Long anterior = ultimaConsulta.get(pedidoId);
		if (anterior != null && agora - anterior < INTERVALO_MIN_CONSULTA_MS) {
			return false;
		}
		ultimaConsulta.put(pedidoId, agora);
		return true;
	}

	// ---------------------------------------------------------------- webhook

	/**
	 * Chamado pelo webhook: consulta o pagamento na API do Asaas e atualiza o pedido. Pode ser chamado
	 * várias vezes para o mesmo pagamento (o Asaas reenvia avisos): aprovado uma vez, o resto é ignorado.
	 *
	 * @throws PagamentoIndisponivelException se a API não respondeu (o webhook devolve erro e o Asaas tenta de novo)
	 */
	public void processarNotificacao(String paymentId) {
		Optional<Pagamento> pagamento = gateway.consultar(paymentId);
		if (pagamento.isEmpty()) {
			log.info("Notificação do Asaas para o pagamento {}, que a API não conhece. Ignorada.", paymentId);
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

		if (!(APROVADO.equals(pg.status()) || "CONFIRMED".equals(pg.status()))) {
			// Pagamento ainda pendente, recusado ou expirado: só guarda a situação do Pix que está em uso
			if (pedido.getAsaasPaymentId() != null && pedido.getAsaasPaymentId().equals(pg.id())) {
				pedido.setAsaasStatus(pg.status());
				pedidoRepository.save(pedido);
			}
			return;
		}
		if (pedido.getAsaasPaymentId() != null && pedido.getAsaasPaymentId().equals(pg.id()) && (APROVADO.equals(pedido.getAsaasStatus()) || "CONFIRMED".equals(pedido.getAsaasStatus()))) {
			return; // aviso repetido: já tratado
		}
		pedido.setAsaasPaymentId(pg.id());
		pedido.setAsaasStatus(pg.status());

		StatusPedido atual = pedido.getStatus();
		if (pg.valor() == null || pg.valor().compareTo(pedido.getTotal()) != 0) {
			log.warn("Pagamento {} aprovado com valor {} diferente do pedido {} ({}).", pg.id(), pg.valor(),
					pedido.getCodigo(), pedido.getTotal());
			pedido.registrarAlteracao(atual, atual, false, null, null, RESPONSAVEL, null,
					"Pagamento #" + pg.id() + " aprovado com valor diferente do pedido. Conferir no Asaas antes de seguir.");
		} else if (atual == StatusPedido.AGUARDANDO_PAGAMENTO) {
			pedido.setStatus(StatusPedido.PAGO);
			pedido.registrarAlteracao(atual, StatusPedido.PAGO, false, null, null, RESPONSAVEL, null,
					"Pagamento aprovado (Asaas #" + pg.id() + ").");
		} else if (atual == StatusPedido.CANCELADO) {
			// Pedido cancelado não é reaberto; pagamento aprovado após cancelamento é reembolsado.
			try {
				gateway.estornar(pg.id());
				pedido.setAsaasStatus("refunded");
				pedido.registrarAlteracao(atual, atual, false, null, null, RESPONSAVEL, null,
						"Pagamento #" + pg.id() + " aprovado após cancelamento; estorno integral solicitado ao Asaas.");
			} catch (RuntimeException e) {
				pedido.setAsaasStatus("approved_refund_pending");
				pedido.registrarAlteracao(atual, atual, false, null, null, RESPONSAVEL, null,
						"Pagamento #" + pg.id() + " aprovado após cancelamento; estorno automático falhou e requer nova tentativa.");
			}
		}
		if (atual == StatusPedido.AGUARDANDO_PAGAMENTO && carteiraService != null) {
			carteiraService.creditarCashback(pedido.getUsuario().getId(), pedido.getSubtotal(), "CASHBACK-PEDIDO-" + pedido.getId());
		}
		pedidoRepository.save(pedido);
	}
}
