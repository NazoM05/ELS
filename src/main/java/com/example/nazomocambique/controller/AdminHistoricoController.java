package com.example.nazomocambique.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.nazomocambique.model.HistoricoPedido;
import com.example.nazomocambique.model.Paginacao;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.repository.HistoricoPedidoRepository;

/**
 * Histórico de alterações dos pedidos: quem mudou cada etapa, quando, e os valores anterior e novo.
 * Só para administradores (regra de /admin/** no SecurityConfig, repetida aqui). Tela somente leitura.
 */
@Controller
@RequestMapping("/admin/historico")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHistoricoController {

	static final int POR_PAGINA = 30;

	private final HistoricoPedidoRepository historicoRepository;

	public AdminHistoricoController(HistoricoPedidoRepository historicoRepository) {
		this.historicoRepository = historicoRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public String listar(@RequestParam(defaultValue = "") String codigo, @RequestParam(defaultValue = "") String usuario,
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
}
