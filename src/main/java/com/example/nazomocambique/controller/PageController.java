package com.example.nazomocambique.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.Ordenacao;
import com.example.nazomocambique.model.Paginacao;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.service.ProdutoService;

@Controller 
public class PageController {

	ProdutoService produtoService;
	public PageController (ProdutoService produtoService) {
		this.produtoService = produtoService;
	}

	@GetMapping({"/", "/home"})
	public String home(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String ordem,
			@RequestParam(required = false) String pagina, Model model) {
		carregarCatalogo(model, "/home", busca, categoria, genero, ordem, pagina);
		return "berzerk";
	}
	
	@GetMapping({"/london"})
	public String london(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String ordem,
			@RequestParam(required = false) String pagina, Model model) {
		carregarCatalogo(model, "/london", busca, categoria, genero, ordem, pagina);
		return "home";
	}

	@GetMapping("/colecao/vice-city")
	public String colecaoViceCity(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String ordem,
			@RequestParam(required = false) String pagina, Model model) {
		return colecao(model, "/colecao/vice-city", "Coleção Vice City", Produto.COLECAO_VICE_CITY,
				busca, categoria, genero, ordem, pagina);
	}

	@GetMapping("/colecao/tricolor")
	public String colecaoTricolor(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String ordem,
			@RequestParam(required = false) String pagina, Model model) {
		return colecao(model, "/colecao/tricolor", "Imortal Tricolor", Produto.COLECAO_TRICOLOR,
				busca, categoria, genero, ordem, pagina);
	}

	@GetMapping("/colecao/urban-culture")
	public String colecaoUrbanCulture(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String ordem,
			@RequestParam(required = false) String pagina, Model model) {
		return colecao(model, "/colecao/urban-culture", "Nazo · Urban Culture Oversized",
				Produto.COLECAO_URBAN_CULTURE, busca, categoria, genero, ordem, pagina);
	}

	/** Página de uma coleção especial: mesma vitrine, só com os produtos da coleção (acesso público). */
	private String colecao(Model model, String caminho, String titulo, String colecao, String busca,
			String categoria, String genero, String ordem, String pagina) {
		carregarCatalogo(model, caminho, busca, categoria, genero, ordem, pagina, false, colecao);
		model.addAttribute("paginaColecao", true);
		model.addAttribute("colecaoTitulo", titulo);
		model.addAttribute("colecaoCaminho", caminho);
		model.addAttribute("colecaoTema", colecao.equals(Produto.COLECAO_TRICOLOR) ? "tricolor" : "padrao");
		return "berzerk";
	}

	@GetMapping("/outlet")
	public String outlet(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero,
			@RequestParam(required = false) String ordem,
			@RequestParam(required = false) String pagina, Model model) {
		carregarCatalogo(model, "/outlet", busca, categoria, genero, ordem, pagina, true);
		model.addAttribute("paginaOutlet", true);
		return "berzerk";
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
			String generoTexto, String ordemTexto, String paginaTexto) {
		carregarCatalogo(model, caminho, busca, categoriaTexto, generoTexto, ordemTexto, paginaTexto, false);
	}

	private void carregarCatalogo(Model model, String caminho, String busca, String categoriaTexto,
			String generoTexto, String ordemTexto, String paginaTexto, boolean outlet) {
		carregarCatalogo(model, caminho, busca, categoriaTexto, generoTexto, ordemTexto, paginaTexto, outlet, null);
	}

	private void carregarCatalogo(Model model, String caminho, String busca, String categoriaTexto,
			String generoTexto, String ordemTexto, String paginaTexto, boolean outlet, String colecao) {
		String termo = busca == null ? "" : busca.trim();
		Categoria categoria = Categoria.deTexto(categoriaTexto);
		Genero genero = Genero.deTexto(generoTexto);
		Ordenacao ordem = Ordenacao.deTexto(ordemTexto);
		Page<Produto> pagina = produtoService.filtrar(termo, categoria, genero, ordem, numeroDaPagina(paginaTexto) - 1, outlet, colecao);

		Map<String, String> filtros = new LinkedHashMap<>();
		filtros.put("busca", termo);
		filtros.put("categoria", categoria == null ? null : categoria.name());
		filtros.put("genero", genero == null ? null : genero.name());
		filtros.put("ordem", ordem == Ordenacao.PADRAO ? null : ordem.getParametro());

		model.addAttribute("listaProdutos", pagina.getContent());
		model.addAttribute("paginacao", new Paginacao(pagina.getNumber() + 1, pagina.getTotalPages(),
				pagina.getTotalElements(), caminho, filtros));
		model.addAttribute("busca", termo);
		model.addAttribute("categoriaSelecionada", categoria);
		model.addAttribute("generoSelecionado", genero);
		model.addAttribute("ordem", ordem);
		model.addAttribute("filtrosAtivos", !termo.isEmpty() || categoria != null || genero != null
				|| ordem != Ordenacao.PADRAO);
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
			model.addAttribute("siteBaseUrl", ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
			
			if (produto != null && produto.getCategoria() != null) {
				// Produto de coleção especial recomenda peças da mesma coleção; os demais, o catálogo normal.
				List<Produto> recomendados = produtoService.filtrar(
					"", 
					produto.getCategoria(), 
					null, 
					Ordenacao.PADRAO,
					0,
					false,
					produto.getColecao()
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
	
	// Lista de desejos: a página só entrega o esqueleto; os itens vêm do localStorage (jshome.js)
	@GetMapping("/favoritos")
	public String favoritos(Model model) {
		// O topo desta página não mostra o atalho do coração (ela já é a lista de desejos)
		model.addAttribute("paginaFavoritos", true);
		return "favoritos";
	}

	@GetMapping("/login")
	public String login(Model model) {
		return "login";
	}
}