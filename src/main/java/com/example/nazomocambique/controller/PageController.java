package com.example.nazomocambique.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.Paginacao;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.service.ProdutoService;

@Controller 
public class PageController {

	ProdutoService produtoService;
	public PageController (ProdutoService produtoService) {
		this.produtoService = produtoService;
	}

	// Catálogo da loja. Filtros combináveis pela URL: /home?busca=jeans&categoria=CALCA&genero=FEMININO
	@GetMapping({"/", "/home"})
	public String home(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String pagina, Model model) {
		carregarCatalogo(model, "/home", busca, categoria, genero, pagina);
		return "berzerk";
	}
	
	@GetMapping({"/london"})
	public String london(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String pagina, Model model) {
		carregarCatalogo(model, "/london", busca, categoria, genero, pagina);
		return "home";
	}

	@ModelAttribute("categorias")
	public Categoria[] categorias() {
		return Categoria.values();
	}

	@ModelAttribute("generosFiltro")
	public List<Genero> generosFiltro() {
		return List.of(Genero.MASCULINO, Genero.FEMININO);
	}

	private void carregarCatalogo(Model model, String caminho, String busca, String categoriaTexto,
			String generoTexto, String paginaTexto) {
		String termo = busca == null ? "" : busca.trim();
		Categoria categoria = Categoria.deTexto(categoriaTexto);
		Genero genero = Genero.deTexto(generoTexto);
		Page<Produto> pagina = produtoService.filtrar(termo, categoria, genero, numeroDaPagina(paginaTexto) - 1);

		Map<String, String> filtros = new LinkedHashMap<>();
		filtros.put("busca", termo);
		filtros.put("categoria", categoria == null ? null : categoria.name());
		filtros.put("genero", genero == null ? null : genero.name());

		model.addAttribute("listaProdutos", pagina.getContent());
		model.addAttribute("paginacao", new Paginacao(pagina.getNumber() + 1, pagina.getTotalPages(),
				pagina.getTotalElements(), caminho, filtros));
		model.addAttribute("busca", termo);
		model.addAttribute("categoriaSelecionada", categoria);
		model.addAttribute("generoSelecionado", genero);
		model.addAttribute("filtrosAtivos", !termo.isEmpty() || categoria != null || genero != null);
	}

	private int numeroDaPagina(String texto) {
		if (texto == null || texto.isBlank()) {
			return 1;
		}
		try {
			return Math.max(Integer.parseInt(texto.trim()), 1);
		} catch (NumberFormatException e) {
			return 1;
		}
	}
  
	@GetMapping({"/cadastroprod"})
	public String cadastrar(Model model) {
		model.addAttribute("listaProdutos", produtoService.listarTodos());
		return "cadastroprod";
	}
	
  // Endereço antigo da pesquisa: segue funcionando e cai no catálogo com filtros
  @GetMapping({"/pesquisa"})
  public String pesquisa(@RequestParam Optional<String> p, RedirectAttributes redirect) {
    p.filter(termo -> !termo.isBlank()).ifPresent(termo -> redirect.addAttribute("busca", termo));
    return "redirect:/home";
  }

	@GetMapping("/detalhes/{id}")
	public String detalhes(@PathVariable Long id, Model model) {
		try {
			Produto produto = produtoService.registrarVisualizacao(id);
			model.addAttribute("produto", produto);
			
			if (produto != null && produto.getCategoria() != null) {
				List<Produto> recomendados = produtoService.filtrar(
					"", 
					produto.getCategoria(), 
					null, 
					0
				).getContent()
				.stream()
				.filter(p -> !p.getId().equals(id))
				.limit(4)
				.toList();
				
				model.addAttribute("produtosRecomendados", recomendados);
			} else {
				model.addAttribute("produtosRecomendados", List.of());
			}
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("produtosRecomendados", List.of());
		}
		return "detalhes";
	}
	
	@GetMapping("/login")
	public String login(Model model) {
		return "login";
	}
}