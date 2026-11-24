package com.example.nazomocambique.controller;
import java.security.Principal;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.nazomocambique.service.*;
import com.example.nazomocambique.model.Usuario;
@Controller
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'USUARIO')")
public class CarteiraController {
 private final CarteiraService carteira; private final ContaService conta;
 public CarteiraController(CarteiraService c, ContaService a){carteira=c;conta=a;}
 @GetMapping("/carteira") public String carteira(Principal p, Model m){Usuario u=conta.buscarPorUsername(p.getName()).orElseThrow();m.addAttribute("saldo",carteira.saldo(u.getId()));m.addAttribute("movimentos",carteira.historico(u.getId()));return "carteira";}
}
