package com.example.nazomocambique.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.nazomocambique.model.Aparencia;
import com.example.nazomocambique.service.AparenciaService;

/** Disponibiliza a identidade visual (cor e logo) como "site" em todos os templates. */
@ControllerAdvice
public class AparenciaAdvice {

	private final AparenciaService aparenciaService;

	public AparenciaAdvice(AparenciaService aparenciaService) {
		this.aparenciaService = aparenciaService;
	}

	@ModelAttribute("site")
	public Aparencia site() {
		return aparenciaService.obter();
	}
}
