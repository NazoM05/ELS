package com.example.nazomocambique.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Para onde vai quem está logado mas tenta abrir uma página que o seu perfil não permite. */
@Controller
public class AcessoNegadoController {

	@GetMapping("/acesso-negado")
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public String acessoNegado() {
		return "acesso-negado";
	}
}
