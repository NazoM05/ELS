package com.example.nazomocambique.config;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.nazomocambique.model.Aparencia;
import com.example.nazomocambique.service.AparenciaService;
import com.example.nazomocambique.service.PersonalizacaoService;
import com.example.nazomocambique.repository.ConfiguracaoCashbackRepository;

/**
 * Disponibiliza em todos os templates:
 *   site               identidade visual (cor e logo)
 *   txt                textos do site (personalizados ou padrão), por chave: ${txt['pg.login.h2']}
 *   anuncios           avisos da barra do topo que não estão em branco
 *   personalizacaoCss  cores personalizadas de cada item e de cada página
 */
@ControllerAdvice
public class AparenciaAdvice {

	private final AparenciaService aparenciaService;
	private final PersonalizacaoService personalizacaoService;
	private final ConfiguracaoCashbackRepository cashbackRepository;

	public AparenciaAdvice(AparenciaService aparenciaService, PersonalizacaoService personalizacaoService, ConfiguracaoCashbackRepository cashbackRepository) {
		this.aparenciaService = aparenciaService;
		this.personalizacaoService = personalizacaoService;
		this.cashbackRepository = cashbackRepository;
	}

	@ModelAttribute("site")
	public Aparencia site() {
		return aparenciaService.obter();
	}

	@ModelAttribute("txt")
	public Map<String, String> txt() {
		return personalizacaoService.textos();
	}

	@ModelAttribute("anuncios")
	public List<String> anuncios() {
		return personalizacaoService.anuncios();
	}

	@ModelAttribute("cashbackAtivo")
	public boolean cashbackAtivo() {
		return cashbackRepository.findById(1L).map(c -> c.isAtivo()).orElse(false);
	}

	@ModelAttribute("personalizacaoCss")
	public String personalizacaoCss() {
		return personalizacaoService.css(aparenciaService.obter());
	}
}
