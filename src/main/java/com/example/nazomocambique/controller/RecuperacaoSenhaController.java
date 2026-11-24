package com.example.nazomocambique.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.nazomocambique.service.RedefinicaoSenhaService;

@Controller
public class RecuperacaoSenhaController {
    private final RedefinicaoSenhaService service;

    public RecuperacaoSenhaController(RedefinicaoSenhaService service) {
        this.service = service;
    }

    @GetMapping("/esqueci-senha")
    public String esqueciSenha() {
        return "esqueci-senha";
    }

    @PostMapping("/esqueci-senha")
    public String solicitar(@RequestParam("email") String email, Model model) {
        try {
            service.solicitar(email);
            model.addAttribute("email", email == null ? "" : email.trim());
            model.addAttribute("sucesso", "Se o e-mail estiver cadastrado e ativo, enviaremos um código de verificação. Confira sua caixa de entrada e spam.");
        } catch (RuntimeException e) {
            model.addAttribute("email", email == null ? "" : email.trim());
            model.addAttribute("erro", "Não foi possível enviar o código agora. Tente novamente mais tarde.");
        }
        return "esqueci-senha";
    }


    @GetMapping("/redefinir-senha")
    public String redefinirForm(@RequestParam(name = "email", required = false) String email, Model model) {
        model.addAttribute("email", email == null ? "" : email.trim());
        return "redefinir-senha";
    }


    @PostMapping("/redefinir-senha")
    public String redefinir(@RequestParam("email") String email,
            @RequestParam("codigo") String codigo,
            @RequestParam("novaSenha") String novaSenha,
            @RequestParam("confirmarSenha") String confirmarSenha,
            Model model) {
        boolean sucesso = service.redefinir(email, codigo, novaSenha, confirmarSenha);
        if (sucesso) {
            return "redirect:/login?senhaRedefinida=true";
        }
        model.addAttribute("email", email == null ? "" : email.trim());
        model.addAttribute("erro", "Código inválido, expirado, já utilizado ou dados de senha incorretos. Confira o código e os requisitos da senha.");
        return "redefinir-senha";
    }
}
