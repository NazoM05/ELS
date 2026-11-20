package com.example.nazomocambique.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.model.CatalogoPersonalizacao;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Campo;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Grupo;
import com.example.nazomocambique.model.CatalogoPersonalizacao.GrupoCores;
import com.example.nazomocambique.model.CatalogoPersonalizacao.ItemCor;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Pagina;
import com.example.nazomocambique.model.CatalogoPersonalizacao.Secao;
import com.example.nazomocambique.service.AparenciaService;
import com.example.nazomocambique.service.PersonalizacaoService;

/**
 * Tela "Configurações" do painel: cor principal e logo, e a personalização de textos e cores do site todo
 * e de cada página. Acesso só para ADMIN (ver SecurityConfig).
 */
@Controller
@RequestMapping("/admin/configuracoes")
public class ConfiguracaoController {

	// Marcadores da prévia ao vivo: a tela troca pela cor escolhida (e o contraste/filtro calculados no navegador)
	private static final String MARCA_COR = "__COR__";
	private static final String MARCA_CONTRASTE = "__CONTRASTE__";
	private static final String MARCA_FILTRO = "__FILTRO__";

	/** Campo de texto como a tela precisa: valor em uso + padrão para o botão "voltar ao padrão". */
	public record CampoTela(String chave, String rotulo, String padrao, String valor, boolean longo, boolean opcional,
			int limite, boolean alterado) {
	}

	public record GrupoTela(String nome, List<CampoTela> campos) {
	}

	/** Cor como a tela precisa: valor salvo (vazio = padrão) e a regra de CSS para a prévia. */
	public record CorTela(String chave, String rotulo, String valor, String regra) {
	}

	public record GrupoCorTela(String nome, List<CorTela> itens, int alterados) {
	}

	public record ItemMenu(Secao secao, int alterados) {
	}

	private final AparenciaService aparenciaService;
	private final PersonalizacaoService personalizacaoService;

	public ConfiguracaoController(AparenciaService aparenciaService, PersonalizacaoService personalizacaoService) {
		this.aparenciaService = aparenciaService;
		this.personalizacaoService = personalizacaoService;
	}

	@GetMapping
	public String tela(@RequestParam(required = false) String secao, Model model) {
		Secao atual = CatalogoPersonalizacao.secao(secao);
		if (atual == null) {
			atual = CatalogoPersonalizacao.secao("aparencia");
		}

		// nome -> hex (o primeiro é o padrão do projeto)
		Map<String, String> cores = new LinkedHashMap<>();
		cores.put("Preto (padrão)", "#0A0A0A");
		cores.put("Vermelho", "#C8102E");
		cores.put("Rosa", "#E0577C");
		cores.put("Laranja", "#E8590C");
		cores.put("Amarelo", "#FACC15");
		cores.put("Verde", "#1F7A4D");
		cores.put("Azul", "#1D4ED8");
		cores.put("Roxo", "#5B2A86");
		model.addAttribute("coresPredefinidas", cores);

		List<ItemMenu> menuGeral = new ArrayList<>();
		List<ItemMenu> menuPaginas = new ArrayList<>();
		for (Secao s : CatalogoPersonalizacao.secoes()) {
			ItemMenu item = new ItemMenu(s, personalizacaoService.alteradosNaSecao(s));
			("pagina".equals(s.tipo()) ? menuPaginas : menuGeral).add(item);
		}
		model.addAttribute("menuGeral", menuGeral);
		model.addAttribute("menuPaginas", menuPaginas);
		model.addAttribute("secaoAtual", atual);

		Pagina pagina = atual.pagina();
		switch (atual.tipo()) {
			case "textos" -> model.addAttribute("gruposTexto", gruposTexto(CatalogoPersonalizacao.textosGerais()));
			case "cores" -> model.addAttribute("gruposCor", gruposCor(null));
			case "pagina" -> {
				model.addAttribute("gruposTexto", gruposTexto(pagina.grupos()));
				model.addAttribute("gruposCor", gruposCor(pagina));
			}
			default -> {
			}
		}
		// Página aberta na prévia: a própria página da seção, ou a vitrine para as seções do site todo
		String previa = pagina != null ? pagina.urlPrevia() : "/home";
		model.addAttribute("urlPrevia", previa);
		return "admin/configuracoes";
	}

	@PostMapping("/salvar")
	public String salvar(@RequestParam String corPrimaria,
			@RequestParam(value = "logo", required = false) MultipartFile logo,
			RedirectAttributes redirect) {
		try {
			aparenciaService.salvar(corPrimaria, logo);
			redirect.addFlashAttribute("sucesso", "Alterações salvas. A nova aparência já vale para todo o site.");
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			redirect.addFlashAttribute("erro", "Não foi possível salvar as alterações. Tente novamente.");
		}
		return "redirect:/admin/configuracoes";
	}

	@PostMapping("/restaurar")
	public String restaurar(RedirectAttributes redirect) {
		try {
			aparenciaService.restaurarPadrao();
			redirect.addFlashAttribute("sucesso", "Cor e logo originais restaurados.");
		} catch (Exception e) {
			e.printStackTrace();
			redirect.addFlashAttribute("erro", "Não foi possível restaurar o padrão. Tente novamente.");
		}
		return "redirect:/admin/configuracoes";
	}

	/** Grava os textos e cores de uma seção (todos os campos dela vêm no formulário, com o nome = chave). */
	@PostMapping("/personalizacao/salvar")
	public String salvarPersonalizacao(@RequestParam String secao, @RequestParam Map<String, String> valores,
			RedirectAttributes redirect) {
		Secao s = CatalogoPersonalizacao.secao(secao);
		if (s == null || "aparencia".equals(s.tipo())) {
			return "redirect:/admin/configuracoes";
		}
		try {
			personalizacaoService.salvarSecao(s, valores);
			redirect.addFlashAttribute("sucesso", "\"" + s.nome() + "\" salvo. As mudanças já aparecem no site.");
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("erro", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			redirect.addFlashAttribute("erro", "Não foi possível salvar as alterações. Tente novamente.");
		}
		redirect.addAttribute("secao", s.id());
		return "redirect:/admin/configuracoes";
	}

	@PostMapping("/personalizacao/restaurar")
	public String restaurarPersonalizacao(@RequestParam String secao, RedirectAttributes redirect) {
		Secao s = CatalogoPersonalizacao.secao(secao);
		if (s == null || "aparencia".equals(s.tipo())) {
			return "redirect:/admin/configuracoes";
		}
		try {
			personalizacaoService.restaurarSecao(s);
			redirect.addFlashAttribute("sucesso", "\"" + s.nome() + "\" voltou ao padrão.");
		} catch (Exception e) {
			e.printStackTrace();
			redirect.addFlashAttribute("erro", "Não foi possível restaurar o padrão. Tente novamente.");
		}
		redirect.addAttribute("secao", s.id());
		return "redirect:/admin/configuracoes";
	}

	private List<GrupoTela> gruposTexto(List<Grupo> grupos) {
		List<GrupoTela> tela = new ArrayList<>();
		for (Grupo g : grupos) {
			List<CampoTela> campos = new ArrayList<>();
			for (Campo c : g.campos()) {
				campos.add(new CampoTela(c.chave(), c.rotulo(), c.padrao(), personalizacaoService.texto(c.chave()),
						c.longo(), c.opcional(), c.limite(), personalizacaoService.salvo(c.chave()) != null));
			}
			tela.add(new GrupoTela(g.nome(), campos));
		}
		return tela;
	}

	private List<GrupoCorTela> gruposCor(Pagina pagina) {
		String escopo = pagina == null ? CatalogoPersonalizacao.ESCOPO_GERAL : pagina.escopo();
		List<GrupoCorTela> tela = new ArrayList<>();
		for (GrupoCores g : CatalogoPersonalizacao.gruposDeCores(pagina != null)) {
			List<CorTela> itens = new ArrayList<>();
			int alterados = 0;
			for (ItemCor item : g.itens()) {
				String chave = CatalogoPersonalizacao.chaveCor(pagina == null ? null : pagina.id(), item.id());
				String salvo = personalizacaoService.salvo(chave);
				if (salvo != null) {
					alterados++;
				}
				itens.add(new CorTela(chave, item.rotulo(), salvo == null ? "" : salvo,
						PersonalizacaoService.regra(item, escopo, MARCA_COR, MARCA_CONTRASTE, MARCA_FILTRO)));
			}
			tela.add(new GrupoCorTela(g.nome(), itens, alterados));
		}
		return tela;
	}
}
