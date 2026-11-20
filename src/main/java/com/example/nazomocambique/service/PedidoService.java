package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.DadosEntrega;
import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.ItemCarrinho;
import com.example.nazomocambique.model.ItemPedido;
import com.example.nazomocambique.model.OpcaoFrete;
import com.example.nazomocambique.model.Pacote;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.model.ResumoCheckout;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.repository.ProdutoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;

/**
 * Finalização da compra: confere o carrinho com o banco (preço, tamanho e estoque),
 * grava o pedido e desconta o estoque de cada tamanho vendido.
 * O preço sempre vem do produto no banco; o que o navegador manda é só id, tamanho e quantidade.
 */
@Service
public class PedidoService {

	/** Frete grátis para a região Sul (PR, SC, RS) a partir deste subtotal. */
	public static final BigDecimal FRETE_GRATIS_SUL = new BigDecimal("99.00");
	/** Frete grátis para os demais estados a partir deste subtotal. */
	public static final BigDecimal FRETE_GRATIS_DEMAIS = new BigDecimal("250.00");
	public static final Set<String> UFS_SUL = Set.of("PR", "SC", "RS");
	/** Frete cobrado abaixo do valor mínimo para frete grátis. */
	public static final BigDecimal VALOR_FRETE = new BigDecimal("19.90");
	/** Limites para o carrinho não virar um pedido absurdo por engano. */
	public static final int MAX_LINHAS = 30;
	public static final int MAX_QUANTIDADE_POR_ITEM = 50;

	private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
			"MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

	private final ProdutoRepository produtoRepository;
	private final PedidoRepository pedidoRepository;
	private final UsuarioRepository usuarioRepository;
	private final FreteService freteService;

	public PedidoService(ProdutoRepository produtoRepository, PedidoRepository pedidoRepository,
			UsuarioRepository usuarioRepository, FreteService freteService) {
		this.produtoRepository = produtoRepository;
		this.pedidoRepository = pedidoRepository;
		this.usuarioRepository = usuarioRepository;
		this.freteService = freteService;
	}

	// ---------------------------------------------------------------- leitura do carrinho

	/**
	 * Converte os parâmetros "item" (formato id:tamanho:quantidade) em itens do carrinho.
	 * Valores malformados são ignorados; o mesmo produto+tamanho repetido é somado numa linha só.
	 */
	public static List<ItemCarrinho> lerItens(List<String> brutos) {
		Map<String, ItemCarrinho> porChave = new LinkedHashMap<>();
		if (brutos == null) {
			return List.of();
		}
		for (String bruto : brutos) {
			if (bruto == null) {
				continue;
			}
			int primeiro = bruto.indexOf(':');
			int ultimo = bruto.lastIndexOf(':');
			if (primeiro <= 0 || ultimo <= primeiro) {
				continue;
			}
			try {
				long id = Long.parseLong(bruto.substring(0, primeiro).trim());
				String tamanho = bruto.substring(primeiro + 1, ultimo).trim();
				int quantidade = Integer.parseInt(bruto.substring(ultimo + 1).trim());
				if (id <= 0 || quantidade <= 0 || tamanho.isEmpty()) {
					continue;
				}
				String chave = id + "|" + Produto.normalizarTamanho(tamanho);
				ItemCarrinho atual = porChave.get(chave);
				int soma = Math.min((atual == null ? 0 : atual.quantidade()) + quantidade, MAX_QUANTIDADE_POR_ITEM);
				porChave.put(chave, new ItemCarrinho(id, atual == null ? tamanho : atual.tamanho(), soma));
			} catch (NumberFormatException e) {
				// item malformado: ignora
			}
			if (porChave.size() >= MAX_LINHAS) {
				break;
			}
		}
		return new ArrayList<>(porChave.values());
	}

	// ---------------------------------------------------------------- resumo (tela)

	/** Só com o estado (sem CEP): frete pela regra fixa da loja. Mantido para quem ainda não tem o CEP. */
	public ResumoCheckout montarResumo(List<ItemCarrinho> itens, String uf) {
		DadosEntrega dados = new DadosEntrega();
		dados.setUf(uf == null ? "" : uf);
		return montarResumo(itens, dados);
	}

	/**
	 * Confere o carrinho sem alterar nada: usado para montar a tela de finalização.
	 * Com CEP e estado informados, o frete vem da cotação dos Correios (se configurados).
	 */
	@Transactional(readOnly = true)
	public ResumoCheckout montarResumo(List<ItemCarrinho> itens, DadosEntrega dados) {
		Map<Long, Optional<Produto>> cache = new HashMap<>();
		List<ResumoCheckout.Linha> linhas = new ArrayList<>();
		for (ItemCarrinho item : itens) {
			Produto produto = cache.computeIfAbsent(item.produtoId(), produtoRepository::findById).orElse(null);
			linhas.add(conferir(item, produto, 0));
		}
		Map<Long, Produto> produtos = new HashMap<>();
		cache.forEach((id, p) -> p.ifPresent(produto -> produtos.put(id, produto)));
		return resumir(linhas, pacoteDe(linhas, produtos), dados);
	}

	// ---------------------------------------------------------------- finalizar

	/**
	 * Grava o pedido e desconta o estoque de cada tamanho vendido, tudo na mesma transação.
	 * Se qualquer item não puder ser vendido (sem estoque suficiente, tamanho inexistente, produto
	 * inativo) ou os dados de entrega forem inválidos, lança CheckoutException e nada é alterado.
	 */
	@Transactional
	public Pedido finalizar(Long usuarioId, DadosEntrega dados, List<ItemCarrinho> itens) {
		Map<String, String> errosCampos = validarEntrega(dados);
		List<String> mensagens = new ArrayList<>();
		if (itens == null || itens.isEmpty()) {
			mensagens.add("Seu carrinho está vazio.");
		}
		if (!errosCampos.isEmpty() || !mensagens.isEmpty()) {
			throw new CheckoutException(errosCampos, mensagens);
		}

		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));

		// Trava os produtos sempre na mesma ordem (por id) para duas compras simultâneas não se bloquearem
		Map<Long, Produto> produtos = new HashMap<>();
		itens.stream().map(ItemCarrinho::produtoId).distinct().sorted(Comparator.naturalOrder())
				.forEach(id -> produtoRepository.buscarParaAtualizar(id).ifPresent(p -> produtos.put(id, p)));

		// Confere tudo antes de mexer no estoque. "reservado" soma o que já foi pedido do mesmo tamanho
		Map<String, Integer> reservado = new HashMap<>();
		List<ResumoCheckout.Linha> linhas = new ArrayList<>();
		for (ItemCarrinho item : itens) {
			Produto produto = produtos.get(item.produtoId());
			String chave = item.produtoId() + "|"
					+ (produto == null ? "" : String.valueOf(produto.resolverTamanho(item.tamanho())));
			ResumoCheckout.Linha linha = conferir(item, produto, reservado.getOrDefault(chave, 0));
			if (linha.getErro() != null) {
				mensagens.add(linha.getNome() + " (" + linha.getTamanho() + "): " + linha.getErro());
			} else {
				reservado.merge(chave, item.quantidade(), Integer::sum);
			}
			linhas.add(linha);
		}
		if (!mensagens.isEmpty()) {
			throw new CheckoutException(Map.of(), mensagens);
		}

		ResumoCheckout resumo = resumir(linhas, pacoteDe(linhas, produtos), dados);
		Pedido pedido = new Pedido();
		pedido.setUsuario(usuario);
		preencherEntrega(pedido, dados);
		pedido.setSubtotal(resumo.getSubtotal());
		pedido.setFrete(resumo.getFrete());
		if (resumo.getFreteEscolhido() != null) {
			pedido.setServicoFrete(resumo.getFreteEscolhido().nome());
			pedido.setPrazoFreteDias(resumo.getFreteEscolhido().prazoDias());
		}
		pedido.setTotal(resumo.getTotal());
		pedido.setCodigo(gerarCodigoUnico());
		pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
		pedido.registrarAlteracao(null, StatusPedido.AGUARDANDO_PAGAMENTO, false, null, null, "Cliente",
				usuario == null ? null : usuario.getUsername(), "Pedido feito na loja.");

		// Baixa do estoque por tamanho + contador de vendidas de cada produto
		for (ResumoCheckout.Linha linha : linhas) {
			Produto produto = produtos.get(linha.getProdutoId());
			String tamanho = linha.getTamanho();
			produto.getEstoque().put(tamanho, produto.estoqueDoTamanho(tamanho) - linha.getQuantidade());
			produto.setVendidas(produto.getVendidasTotal() + linha.getQuantidade());

			ItemPedido item = new ItemPedido();
			item.setProduto(produto);
			item.setNomeProduto(produto.getNome());
			item.setTamanho(tamanho);
			item.setQuantidade(linha.getQuantidade());
			item.setPrecoUnitario(linha.getPrecoUnitario());
			item.setImagem(produto.getImagemFrente());
			pedido.adicionarItem(item);
		}
		produtoRepository.saveAll(produtos.values());
		return pedidoRepository.save(pedido);
	}

	/** Pedido só é mostrado para o dono. */
	@Transactional(readOnly = true)
	public Optional<Pedido> buscarDoUsuario(Long pedidoId, Long usuarioId) {
		return pedidoRepository.findByIdAndUsuarioId(pedidoId, usuarioId);
	}

	// ---------------------------------------------------------------- apoio

	/** Sorteia um código novo; repete no caso raro de já existir um igual. */
	private String gerarCodigoUnico() {
		for (int tentativa = 0; tentativa < 20; tentativa++) {
			String codigo = CodigoPedido.gerar(LocalDate.now());
			if (!pedidoRepository.existsByCodigo(codigo)) {
				return codigo;
			}
		}
		throw new IllegalStateException("Não foi possível gerar um código de pedido único.");
	}

	/**
	 * Confere uma linha do carrinho com o produto do banco. "jaReservado" = unidades do mesmo
	 * produto+tamanho que outra linha do mesmo pedido já está levando.
	 */
	private ResumoCheckout.Linha conferir(ItemCarrinho item, Produto produto, int jaReservado) {
		if (produto == null) {
			return new ResumoCheckout.Linha(item.produtoId(), "Produto removido", null, item.tamanho(),
					item.quantidade(), BigDecimal.ZERO, 0, "Este produto não existe mais na loja.");
		}
		BigDecimal preco = precoDe(produto);
		if (!produto.isStatus()) {
			return new ResumoCheckout.Linha(produto.getId(), produto.getNome(), produto.getImagemFrente(),
					item.tamanho(), item.quantidade(), preco, 0, "Este produto não está mais à venda.");
		}
		String tamanho = produto.resolverTamanho(item.tamanho());
		if (tamanho == null) {
			return new ResumoCheckout.Linha(produto.getId(), produto.getNome(), produto.getImagemFrente(),
					item.tamanho(), item.quantidade(), preco, 0, "Este tamanho não está disponível para o produto.");
		}
		int disponivel = Math.max(produto.estoqueDoTamanho(tamanho) - jaReservado, 0);
		String erro = null;
		if (disponivel == 0) {
			erro = "Esgotado neste tamanho.";
		} else if (item.quantidade() > disponivel) {
			erro = disponivel == 1 ? "Só resta 1 unidade neste tamanho." : "Só restam " + disponivel + " unidades neste tamanho.";
		}
		return new ResumoCheckout.Linha(produto.getId(), produto.getNome(), produto.getImagemFrente(), tamanho,
				item.quantidade(), preco, disponivel, erro);
	}

	private ResumoCheckout resumir(List<ResumoCheckout.Linha> linhas, Pacote pacote, DadosEntrega dados) {
		String uf = dados.getUf();
		BigDecimal subtotal = linhas.stream().filter(l -> l.getErro() == null).map(ResumoCheckout.Linha::getTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
		List<OpcaoFrete> opcoes = freteService.cotar(dados.getCep(), uf, subtotal, pacote);
		OpcaoFrete escolhida = FreteService.escolher(opcoes, dados.getServicoFrete()).orElse(null);
		// Sem opção (estado em branco ou carrinho sem itens vendáveis): vale a regra fixa de sempre
		BigDecimal frete = escolhida != null ? escolhida.valor() : calcularFrete(subtotal, uf);
		BigDecimal minimo = minimoFreteGratis(uf);
		BigDecimal falta = minimo == null || subtotal.signum() == 0 || subtotal.compareTo(minimo) >= 0 ? null
				: minimo.subtract(subtotal);
		return new ResumoCheckout(linhas, subtotal, frete, falta, opcoes, escolhida);
	}

	/** Peso e medidas do que vai na caixa: só as linhas que podem ser vendidas entram. */
	private static Pacote pacoteDe(List<ResumoCheckout.Linha> linhas, Map<Long, Produto> produtos) {
		Map<Categoria, Integer> unidades = new EnumMap<>(Categoria.class);
		for (ResumoCheckout.Linha linha : linhas) {
			if (linha.getErro() != null) {
				continue;
			}
			Produto produto = produtos.get(linha.getProdutoId());
			Categoria categoria = produto == null || produto.getCategoria() == null ? Categoria.CAMISETA : produto.getCategoria();
			unidades.merge(categoria, linha.getQuantidade(), Integer::sum);
		}
		return EmbalagemFrete.montar(unidades);
	}

	/** Subtotal mínimo para frete grátis no estado: R$ 99 no Sul, R$ 250 no resto do Brasil; null = estado não informado. */
	public static BigDecimal minimoFreteGratis(String uf) {
		String sigla = uf == null ? "" : uf.trim().toUpperCase();
		if (!UFS.contains(sigla)) {
			return null;
		}
		return UFS_SUL.contains(sigla) ? FRETE_GRATIS_SUL : FRETE_GRATIS_DEMAIS;
	}

	/**
	 * Frete grátis a partir do mínimo do estado; abaixo disso, valor fixo. Carrinho vazio não tem frete.
	 * Devolve null enquanto o estado não foi informado (a tela mostra "Informe o estado").
	 */
	public static BigDecimal calcularFrete(BigDecimal subtotal, String uf) {
		BigDecimal minimo = minimoFreteGratis(uf);
		if (minimo == null) {
			return null;
		}
		if (subtotal.signum() == 0 || subtotal.compareTo(minimo) >= 0) {
			return BigDecimal.ZERO.setScale(2);
		}
		return VALOR_FRETE;
	}

	// float -> BigDecimal pelo texto, para 74.9f virar 74.90 (e não 74.9000015...)
	private static BigDecimal precoDe(Produto produto) {
		return new BigDecimal(Float.toString(produto.getPreco())).setScale(2, RoundingMode.HALF_UP);
	}

	private Map<String, String> validarEntrega(DadosEntrega d) {
		Map<String, String> erros = new LinkedHashMap<>();
		String nome = limpar(d.getNome());
		if (nome.length() < 2 || nome.length() > 80) {
			erros.put("nome", "Informe o nome de quem vai receber (2 a 80 caracteres).");
		}
		String telefone = digitos(d.getTelefone());
		if (telefone.length() < 10 || telefone.length() > 11) {
			erros.put("telefone", "Informe um telefone com DDD, ex.: (51) 99999-9999.");
		}
		if (digitos(d.getCep()).length() != 8) {
			erros.put("cep", "O CEP deve ter 8 números.");
		}
		obrigatorio(erros, "endereco", d.getEndereco(), 150, "Informe a rua ou avenida.");
		obrigatorio(erros, "numero", d.getNumero(), 20, "Informe o número (ou \"s/n\").");
		if (limpar(d.getComplemento()).length() > 60) {
			erros.put("complemento", "O complemento pode ter até 60 caracteres.");
		}
		obrigatorio(erros, "bairro", d.getBairro(), 80, "Informe o bairro.");
		obrigatorio(erros, "cidade", d.getCidade(), 80, "Informe a cidade.");
		if (!UFS.contains(limpar(d.getUf()).toUpperCase())) {
			erros.put("uf", "Escolha o estado.");
		}
		if (FormaPagamento.deTexto(d.getFormaPagamento()) == null) {
			erros.put("formaPagamento", "Escolha a forma de pagamento.");
		}
		return erros;
	}

	private void preencherEntrega(Pedido p, DadosEntrega d) {
		p.setNomeDestinatario(limpar(d.getNome()));
		p.setTelefone(digitos(d.getTelefone()));
		p.setCep(digitos(d.getCep()));
		p.setEndereco(limpar(d.getEndereco()));
		p.setNumero(limpar(d.getNumero()));
		String complemento = limpar(d.getComplemento());
		p.setComplemento(complemento.isEmpty() ? null : complemento);
		p.setBairro(limpar(d.getBairro()));
		p.setCidade(limpar(d.getCidade()));
		p.setUf(limpar(d.getUf()).toUpperCase());
		p.setFormaPagamento(FormaPagamento.deTexto(d.getFormaPagamento()));
	}

	private static void obrigatorio(Map<String, String> erros, String campo, String valor, int max, String mensagem) {
		String v = limpar(valor);
		if (v.isEmpty()) {
			erros.put(campo, mensagem);
		} else if (v.length() > max) {
			erros.put(campo, "Use no máximo " + max + " caracteres.");
		}
	}

	private static String limpar(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private static String digitos(String valor) {
		return valor == null ? "" : valor.replaceAll("\\D", "");
	}
}
