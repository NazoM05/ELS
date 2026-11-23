package com.example.nazomocambique.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.example.nazomocambique.model.HistoricoPedido;
import com.example.nazomocambique.model.Paginacao;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.repository.HistoricoPedidoRepository;
import com.example.nazomocambique.service.CodigoPedido;
import com.example.nazomocambique.service.ContaService;
import com.example.nazomocambique.service.GestaoPedidoService;
import com.example.nazomocambique.service.GestaoPedidoService.AtualizacaoPedidoException;

/**
 * Telas do administrador sobre pedidos, todas no mesmo controller:
 * <ul>
 * <li>/admin/pedidos: lista, busca pelo código e acompanhamento (etapa, rastreio e anotações);</li>
 * <li>/admin/separacao: quadro Kanban de separação (Picking &amp; Packing), para ADMIN e GESTOR. A página consulta
 * /admin/separacao/pedidos a cada poucos segundos, então cada compra nova aparece sozinha. Entregues e
 * cancelados ficam fora do quadro (continuam na aba "Pedidos");</li>
 * <li>/admin/historico: histórico de alterações (quem mudou cada etapa, quando, valores anterior e novo), só
 * leitura e só ADMIN.</li>
 * </ul>
 * O acesso é decidido no SecurityConfig por URL (/admin/separacao/** para ADMIN e GESTOR, o resto de /admin/**
 * só ADMIN) e repetido aqui, por método, na separação e no histórico, para que continuem protegidos mesmo se a
 * regra de URL for alterada por engano.
 */
@Controller
public class AdminPedidoController {

	// ---------------------------------------------------------------- separação (Kanban)
	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final List<StatusPedido> ETAPAS = List.of(StatusPedido.PAGO, StatusPedido.EM_SEPARACAO,
			StatusPedido.SEPARADO, StatusPedido.ENVIADO);

	// ---------------------------------------------------------------- histórico
	static final int POR_PAGINA = 30;

	private final GestaoPedidoService gestaoPedidoService;
	private final ContaService contaService;
	private final HistoricoPedidoRepository historicoRepository;

	public AdminPedidoController(GestaoPedidoService gestaoPedidoService, ContaService contaService,
			HistoricoPedidoRepository historicoRepository) {
		this.gestaoPedidoService = gestaoPedidoService;
		this.contaService = contaService;
		this.historicoRepository = historicoRepository;
	}

	public record CartaoPedido(String codigo, String cliente, String destino, String confirmadoEm, String confirmadoEmTexto,
			String status, String statusRotulo, String statusClasse, int itens) {
	}

	// ================================================================ /admin/pedidos

	@GetMapping("/admin/pedidos")
	public String listar(Model model) {
		List<Pedido> pedidos = gestaoPedidoService.listarTodos();
		model.addAttribute("statusPedido", StatusPedido.values());
		model.addAttribute("pedidos", pedidos);
		model.addAttribute("contagem", GestaoPedidoService.contarPorStatus(pedidos));
		model.addAttribute("emAberto", pedidos.stream().filter(p -> p.getStatus().isEmAberto()).count());
		return "admin/pedido/listar";
	}

	/** Busca exata pelo código (o campo do topo): achou, abre o pedido; senão volta com aviso. */
	@GetMapping("/admin/pedidos/buscar")
	public String buscar(@RequestParam(required = false) String codigo, RedirectAttributes redirect) {
		return gestaoPedidoService.buscarPorCodigo(codigo)
				.map(p -> "redirect:/admin/pedidos/" + p.getCodigo())
				.orElseGet(() -> {
					redirect.addAttribute("naoEncontrado", CodigoPedido.normalizar(codigo));
					return "redirect:/admin/pedidos";
				});
	}

	@GetMapping("/admin/pedidos/{codigo}")
	public String detalhes(@PathVariable String codigo, Model model) {
		Pedido pedido = gestaoPedidoService.buscarPorCodigo(codigo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
		model.addAttribute("statusPedido", StatusPedido.values());
		model.addAttribute("pedido", pedido);
		model.addAttribute("whatsappUrl", montarWhatsappUrl(pedido));
		return "admin/pedido/detalhes";
	}

	@PostMapping("/admin/pedidos/{codigo}/atualizar")
	public String atualizar(@PathVariable String codigo, @RequestParam(required = false) String status,
			@RequestParam(required = false) String codigoRastreio, @RequestParam(required = false) String observacao,
			Principal principal, RedirectAttributes redirect) {
		String destino = "redirect:/admin/pedidos/" + UriUtils.encodePathSegment(CodigoPedido.normalizar(codigo), "UTF-8");
		try {
			boolean mudou = gestaoPedidoService.atualizar(codigo, StatusPedido.deTexto(status), codigoRastreio, observacao,
					responsavel(principal), principal.getName());
			redirect.addAttribute(mudou ? "sucesso" : "semMudanca", "");
		} catch (AtualizacaoPedidoException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
			redirect.addFlashAttribute("observacaoDigitada", observacao);
		}
		return destino;
	}

	// ================================================================ /admin/separacao

	@GetMapping("/admin/separacao")
	@PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
	public String telaSeparacao() {
		return "admin/pedido/separacao";
	}

	@GetMapping("/admin/separacao/pedidos")
	@ResponseBody
	@PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
	public List<CartaoPedido> pedidosSeparacao() {
		return gestaoPedidoService.listarTodos().stream()
				.filter(p -> p.getStatus() != StatusPedido.ENTREGUE && p.getStatus() != StatusPedido.CANCELADO)
				.map(AdminPedidoController::cartao)
				.toList();
	}

	@PostMapping("/admin/separacao/{codigo}/status")
	@ResponseBody
	@PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
	public ResponseEntity<Map<String, String>> mudarStatusSeparacao(@PathVariable String codigo,
			@RequestParam String status, Principal principal) {
		StatusPedido novo = StatusPedido.deTexto(status);
		if (novo == null || !ETAPAS.contains(novo)) {
			return ResponseEntity.badRequest().body(Map.of("erro", "Etapa inválida."));
		}
		try {
			Pedido atual = gestaoPedidoService.buscarPorCodigo(codigo).orElse(null);
			if (atual == null) {
				return ResponseEntity.status(404).body(Map.of("erro", "Pedido não encontrado."));
			}
			gestaoPedidoService.atualizar(codigo, novo, atual.getCodigoRastreio(), "Alterado na tela de separação.",
					responsavel(principal), principal.getName());
			return ResponseEntity.ok(Map.of("status", novo.name()));
		} catch (AtualizacaoPedidoException e) {
			return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
		}
	}

	// ================================================================ /admin/historico

	@GetMapping("/admin/historico")
	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public String listarHistorico(@RequestParam(defaultValue = "") String codigo, @RequestParam(defaultValue = "") String usuario,
			@RequestParam(defaultValue = "") String etapa, @RequestParam(defaultValue = "") String de,
			@RequestParam(defaultValue = "") String ate, @RequestParam(defaultValue = "false") boolean soEtapas,
			@RequestParam(defaultValue = "1") int pagina, Model model) {

		String codigoBusca = codigo.isBlank() ? null
				: "%" + codigo.trim().toUpperCase(Locale.ROOT).replace("-", "").replaceAll("\\s", "") + "%";
		String usuarioBusca = usuario.isBlank() ? null : "%" + usuario.trim().toLowerCase(Locale.ROOT) + "%";
		StatusPedido etapaBusca = StatusPedido.deTexto(etapa);
		LocalDate dataDe = data(de);
		LocalDate dataAte = data(ate);

		Page<HistoricoPedido> resultado = historicoRepository.buscar(codigoBusca, usuarioBusca, etapaBusca,
				dataDe == null ? null : dataDe.atStartOfDay(), dataAte == null ? null : dataAte.plusDays(1).atStartOfDay(),
				soEtapas, PageRequest.of(Math.max(pagina, 1) - 1, POR_PAGINA));
		// Carrega o código do pedido ainda dentro da transação (a tela mostra e linka para ele)
		resultado.forEach(h -> h.getPedido().getCodigo());

		Map<String, String> filtros = new LinkedHashMap<>();
		filtros.put("codigo", codigo.trim());
		filtros.put("usuario", usuario.trim());
		filtros.put("etapa", etapaBusca == null ? "" : etapaBusca.name());
		filtros.put("de", dataDe == null ? "" : dataDe.toString());
		filtros.put("ate", dataAte == null ? "" : dataAte.toString());
		filtros.put("soEtapas", soEtapas ? "true" : "");

		model.addAttribute("registros", resultado.getContent());
		model.addAttribute("total", resultado.getTotalElements());
		model.addAttribute("paginacao", new Paginacao(resultado.getNumber() + 1, resultado.getTotalPages(),
				resultado.getTotalElements(), "/admin/historico", filtros));
		model.addAttribute("filtros", filtros);
		model.addAttribute("temFiltro", filtros.values().stream().anyMatch(v -> !v.isBlank()));
		model.addAttribute("statusPedido", StatusPedido.values());
		return "admin/historico";
	}

	// ================================================================ apoio

	private static LocalDate data(String texto) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(texto.trim());
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static CartaoPedido cartao(Pedido p) {
		String destino = (p.getCidade() == null ? "" : p.getCidade()) + (p.getUf() == null ? "" : "/" + p.getUf());
		return new CartaoPedido(p.getCodigo(), p.getNomeDestinatario(), destino,
				p.getCriadoEm() == null ? null : p.getCriadoEm().format(ISO), p.getCriadoEmFormatado(),
				p.getStatus().name(), p.getStatus().getRotulo(), p.getStatus().getClasse(), p.getQuantidadeTotal());
	}

	private String montarWhatsappUrl(Pedido pedido) {
		String telefone = pedido.getTelefone() == null ? "" : pedido.getTelefone().replaceAll("\\D", "");
		if (telefone.isBlank()) {
			return "#";
		}
		if (telefone.startsWith("0")) {
			telefone = telefone.substring(1);
		}
		if (!telefone.startsWith("55")) {
			telefone = "55" + telefone;
		}
		String nome = pedido.getNomeDestinatario() == null ? "cliente" : pedido.getNomeDestinatario().trim();
		String mensagem = "Olá " + nome + ", sobre seu pedido " + pedido.getCodigo() + ".";
		return "https://wa.me/" + telefone + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
	}

	/** Nome de quem fez a alteração, para o histórico do pedido. */
	private String responsavel(Principal principal) {
		return contaService.buscarPorUsername(principal.getName())
				.map(u -> u.getNome() != null && !u.getNome().isBlank() ? u.getNome() : u.getUsername())
				.orElse(principal.getName());
	}
}
