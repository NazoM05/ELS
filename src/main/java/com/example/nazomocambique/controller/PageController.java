package com.example.nazomocambique.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Genero;
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
			@RequestParam(required = false) String genero, Model model) {
		carregarCatalogo(model, busca, categoria, genero);
		return "berzerk";
	}
	
	@GetMapping({"/london"})
	public String london(@RequestParam(required = false) String busca,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String genero, Model model) {
		carregarCatalogo(model, busca, categoria, genero);
		return "home";
	}

	// Opções dos filtros da loja: todas as categorias e os públicos Masculino / Feminino
	// (o Unissex entra nos dois resultados, por isso não é um botão de filtro).
	@ModelAttribute("categorias")
	public Categoria[] categorias() {
		return Categoria.values();
	}

	@ModelAttribute("generosFiltro")
	public List<Genero> generosFiltro() {
		return List.of(Genero.MASCULINO, Genero.FEMININO);
	}

	private void carregarCatalogo(Model model, String busca, String categoriaTexto, String generoTexto) {
		String termo = busca == null ? "" : busca.trim();
		Categoria categoria = Categoria.deTexto(categoriaTexto);
		Genero genero = Genero.deTexto(generoTexto);
		List<Produto> produtos = produtoService.filtrar(termo, categoria, genero);

		model.addAttribute("listaProdutos", produtos);
		model.addAttribute("busca", termo);
		model.addAttribute("categoriaSelecionada", categoria);
		model.addAttribute("generoSelecionado", genero);
		model.addAttribute("filtrosAtivos", !termo.isEmpty() || categoria != null || genero != null);
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
			model.addAttribute("produto", produtoService.registrarVisualizacao(id));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "detalhes";
	}
	
	@GetMapping("/login")
	public String login(Model model) {
		return "login";
	}
	
}
