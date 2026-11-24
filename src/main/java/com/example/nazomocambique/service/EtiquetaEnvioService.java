package com.example.nazomocambique.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.config.CorreiosProperties;
import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.ItemPedido;
import com.example.nazomocambique.model.Pacote;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.PrePostagem;
import com.example.nazomocambique.model.SolicitacaoPostagem;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.repository.PedidoRepository;

/**
 * Etiqueta de envio dos Correios (etapa 5). A etiqueta usa a pré-postagem criada na API dos Correios: o código de
 * rastreamento vem sempre da resposta deles. Se a integração não estiver configurada ou o pedido não usar um serviço
 * dos Correios, nada é gerado (nunca se monta um código "de mentira").
 *
 * O pedido fica travado durante a chamada para dois cliques seguidos não criarem duas pré-postagens.
 */
@Service
public class EtiquetaEnvioService {

	private static final Logger log = LoggerFactory.getLogger(EtiquetaEnvioService.class);

	private final PedidoRepository pedidoRepository;
	private final PostagemCorreios correios;
	private final CorreiosProperties props;

	public EtiquetaEnvioService(PedidoRepository pedidoRepository, PostagemCorreios correios, CorreiosProperties props) {
		this.pedidoRepository = pedidoRepository;
		this.correios = correios;
		this.props = props;
	}

	/** Erro de validação mostrado na tela do pedido. */
	public static class EtiquetaException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public EtiquetaException(String mensagem) {
			super(mensagem);
		}
	}

	/** Código do serviço dos Correios do pedido (03298...), ou vazio quando ele usa a entrega padrão da loja. */
	public static String codigoServico(Pedido p) {
		String codigo = p.getServicoFreteCodigo();
		if (codigo == null || codigo.isBlank()) {
			codigo = FreteService.codigoCorreios(p.getServicoFrete()).orElse("");
		}
		return codigo.matches("\\d{5}") ? codigo : "";
	}

	/** Por que a etiqueta ainda não pode ser gerada; null quando já pode (ou já foi gerada). */
	public String motivoBloqueio(Pedido p) {
		if (p.isEtiquetaGerada()) {
			return null;
		}
		if (p.getStatus() != StatusPedido.PAGO && p.getStatus() != StatusPedido.EM_SEPARACAO
				&& p.getStatus() != StatusPedido.SEPARADO) {
			return switch (p.getStatus()) {
				case AGUARDANDO_PAGAMENTO -> "Aguarde a confirmação do pagamento para gerar a etiqueta.";
				case CANCELADO -> "Pedido cancelado: não há etiqueta para gerar.";
				default -> "O pedido já saiu para entrega.";
			};
		}
		if (codigoServico(p).isEmpty()) {
			return "Este pedido usa a entrega padrão da loja, sem serviço dos Correios: não há etiqueta dos Correios para ele.";
		}
		if (!props.configurado()) {
			return "A integração com os Correios não está ligada neste ambiente (correios.enabled e credenciais).";
		}
		if (!props.remetente().completo()) {
			return "Faltam os dados do remetente da loja (correios.remetente.*: nome, CPF/CNPJ e endereço).";
		}
		if (p.getCep() == null || p.getCep().length() != 8 || vazio(p.getEndereco()) || vazio(p.getNumero())
				|| vazio(p.getBairro()) || vazio(p.getCidade()) || p.getUf() == null || p.getUf().length() != 2) {
			return "O endereço do pedido está incompleto para os Correios (CEP, rua, número, bairro, cidade e UF).";
		}
		return null;
	}

	/**
	 * Cria a pré-postagem nos Correios e guarda o código do objeto no pedido. Se já existir, devolve a que existe
	 * (imprimir de novo não gera outro código).
	 */
	@Transactional
	public PrePostagem gerar(String codigo, String responsavel, String login) {
		Pedido p = pedidoRepository.buscarParaAtualizar(CodigoPedido.normalizar(codigo))
				.orElseThrow(() -> new EtiquetaException("Pedido não encontrado."));
		if (p.isEtiquetaGerada()) {
			return new PrePostagem(p.getCorreiosIdPrePostagem(), p.getCorreiosCodigoObjeto());
		}
		String bloqueio = motivoBloqueio(p);
		if (bloqueio != null) {
			throw new EtiquetaException(bloqueio);
		}

		SolicitacaoPostagem solicitacao = new SolicitacaoPostagem(codigoServico(p), destino(p), pacoteDe(p), itensDe(p));
		PrePostagem resultado;
		try {
			resultado = correios.criarPrePostagem(solicitacao);
		} catch (CorreiosIndisponivelException e) {
			log.warn("Pré-postagem do pedido {} não criada: {}", p.getCodigo(), e.getMessage());
			throw new EtiquetaException("Não foi possível gerar a etiqueta nos Correios. " + e.getMessage());
		}
		if (!CodigoBarras.codigoObjetoValido(resultado.codigoObjeto())) {
			// Resposta fora do padrão: não grava nada que o separador possa imprimir como se fosse válido
			log.error("Pedido {}: código de objeto fora do padrão na pré-postagem {}.", p.getCodigo(), resultado.idPrePostagem());
			throw new EtiquetaException("Os Correios devolveram um código de rastreamento fora do padrão. Pré-postagem "
					+ resultado.idPrePostagem() + ": confira no portal dos Correios antes de tentar de novo.");
		}

		p.setCorreiosIdPrePostagem(resultado.idPrePostagem());
		p.setCorreiosCodigoObjeto(resultado.codigoObjeto());
		p.setEtiquetaGeradaEm(java.time.LocalDateTime.now());
		String quem = responsavel == null || responsavel.isBlank() ? "Equipe" : responsavel.trim();
		p.registrarAlteracao(p.getStatus(), p.getStatus(), false, null, null, quem.length() > 80 ? quem.substring(0, 80) : quem,
				login == null || login.isBlank() ? null : (login.length() > 120 ? login.substring(0, 120) : login),
				"Etiqueta dos Correios gerada. Objeto " + resultado.codigoObjeto() + " (pré-postagem " + resultado.idPrePostagem() + ").");
		pedidoRepository.save(p);
		return resultado;
	}

	// ---------------------------------------------------------------- apoio

	private static SolicitacaoPostagem.Destino destino(Pedido p) {
		String tel = p.getTelefone() == null ? "" : p.getTelefone().replaceAll("\\D", "");
		boolean telefoneValido = tel.length() == 10 || tel.length() == 11;
		return new SolicitacaoPostagem.Destino(p.getNomeDestinatario(), telefoneValido ? tel.substring(0, 2) : "",
				telefoneValido ? tel.substring(2) : "", p.getCep(), p.getEndereco(), p.getNumero(),
				p.getComplemento() == null ? "" : p.getComplemento(), p.getBairro(), p.getCidade(), p.getUf());
	}

	private static Pacote pacoteDe(Pedido p) {
		Map<Categoria, Integer> unidades = new EnumMap<>(Categoria.class);
		for (ItemPedido item : p.getItens()) {
			// Mesma regra do checkout: produto apagado ou sem categoria conta como camiseta
			Categoria c = item.getProduto() == null || item.getProduto().getCategoria() == null ? Categoria.CAMISETA
					: item.getProduto().getCategoria();
			unidades.merge(c, item.getQuantidade(), Integer::sum);
		}
		return EmbalagemFrete.montar(unidades);
	}

	private static List<SolicitacaoPostagem.Item> itensDe(Pedido p) {
		return p.getItens().stream()
				.map(i -> new SolicitacaoPostagem.Item(i.getNomeProduto() + " (" + i.getTamanho() + ")", i.getQuantidade(),
						i.getPrecoUnitario()))
				.toList();
	}

	private static boolean vazio(String s) {
		return s == null || s.isBlank();
	}
}
