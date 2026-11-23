package com.example.nazomocambique.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.nazomocambique.model.Categoria;
import com.example.nazomocambique.model.Genero;
import com.example.nazomocambique.model.Produto;
import com.example.nazomocambique.service.ProdutoService;

@Controller
@RequestMapping("/admin")
public class AdminController {

	// Pasta onde as imagens enviadas são gravadas (servida em /img/** pelo ImagensWebConfig)
	private static final Path PASTA_IMAGENS = Paths.get("uploads/img");

	ProdutoService produtoService;

	public AdminController(ProdutoService produtoService) {
		this.produtoService = produtoService;
	}

	// Opções dos selects de Categoria e Gênero (telas de cadastro e edição)
	@ModelAttribute("categorias")
	public Categoria[] categorias() {
		return Categoria.values();
	}

	@ModelAttribute("generos")
	public Genero[] generos() {
		return Genero.values();
	}

	@GetMapping({"/produto/listar"})
	public String listar(Model model) {
		model.addAttribute("listaProdutos", produtoService.listarTodos());
		return "admin/produto/listar";
	}

	@GetMapping({"/produto/cadastrar"})
	public String cadastrar(Model model) {
		model.addAttribute("produto", new Produto());
		model.addAttribute("listaProdutos", produtoService.listarTodos());
		return "admin/produto/cadastrar";
	}

	// Tela de visualização: dados completos + insights e métricas do produto
	@GetMapping({"/produto/visualizar/{id}"})
	public String visualizar(Model model, @PathVariable Long id) {
		try {
			model.addAttribute("produto", produtoService.buscarPorId(id));
			return "admin/produto/visualizar";
		} catch (Exception e) {
			return "redirect:/admin/produto/listar?erro=naoEncontrado";
		}
	}

	@GetMapping({"/produto/editar/{id}"})
	public String editar(Model model, @PathVariable Long id) {
		try {
			model.addAttribute("produto", produtoService.buscarPorId(id));
			return "admin/produto/form";
		} catch (Exception e) {
			return "redirect:/admin/produto/listar?erro=naoEncontrado";
		}
	}

	// Usado pelo cadastrar (id nulo) e pelo editar (id preenchido)
	@PostMapping({"/produto/salvar"})
	public String salvar(Produto produto,
			@RequestParam(value = "arquivoImagemFrente", required = false) MultipartFile arquivoImagemFrente,
			@RequestParam(value = "arquivoImagemCostas", required = false) MultipartFile arquivoImagemCostas,
			@RequestParam(value = "arquivoImagemModelo", required = false) MultipartFile arquivoImagemModelo,
			@RequestParam(value = "arquivoImagemGuiaTamanhos", required = false) MultipartFile arquivoImagemGuiaTamanhos) {
		try {
			// Arquivo novo substitui a imagem; sem arquivo, vale o nome que veio no campo oculto
			// (edição mantém a imagem atual; campo oculto vazio vira "sem imagem").
			produto.setImagemFrente(resolverImagem(arquivoImagemFrente, produto.getImagemFrente()));
			produto.setImagemCostas(resolverImagem(arquivoImagemCostas, produto.getImagemCostas()));
			produto.setImagemModelo(resolverImagem(arquivoImagemModelo, produto.getImagemModelo()));
			produto.setImagemGuiaTamanhos(resolverImagem(arquivoImagemGuiaTamanhos, produto.getImagemGuiaTamanhos()));

			produtoService.salvarFormulario(produto);
			return "redirect:/admin/produto/listar?sucesso";
		} catch (IllegalArgumentException e) {
			// categoria/gênero ausentes, preço negativo ou arquivo que não é imagem
			e.printStackTrace();
			return "redirect:/admin/produto/listar?erro=invalido";
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/admin/produto/listar?erro=salvar";
		}
	}

	// Ativar / inativar (botões da listagem)
	@PostMapping({"/produto/status/{id}"})
	public String alterarStatus(@PathVariable Long id, @RequestParam boolean status) {
		try {
			Produto produto = produtoService.buscarPorId(id);
			produto.setStatus(status);
			produtoService.salvarProduto(produto);
			return "redirect:/admin/produto/listar?sucesso";
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/admin/produto/listar?erro=status";
		}
	}

	// Novo arquivo -> grava e devolve o nome gerado; sem arquivo -> mantém o nome atual (ou null se vazio)
	private String resolverImagem(MultipartFile arquivo, String nomeAtual) throws IOException {
		if (arquivo != null && !arquivo.isEmpty()) {
			return gravarImagem(arquivo);
		}
		return (nomeAtual == null || nomeAtual.isBlank()) ? null : nomeAtual;
	}

	private String gravarImagem(MultipartFile arquivo) throws IOException {
		String tipo = arquivo.getContentType();
		if (tipo == null || !tipo.startsWith("image/")) {
			throw new IllegalArgumentException("O arquivo enviado não é uma imagem.");
		}
		String original = arquivo.getOriginalFilename();
		String extensao = (original != null && original.contains("."))
				? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
		if (!extensao.matches("\\.[a-z0-9]{1,5}")) {
			extensao = ""; // evita extensões estranhas no nome do arquivo gravado
		}
		String nome = UUID.randomUUID() + extensao;
		Files.createDirectories(PASTA_IMAGENS);
		try (InputStream entrada = arquivo.getInputStream()) {
			Files.copy(entrada, PASTA_IMAGENS.resolve(nome));
		}
		return nome;
	}
}
