package com.example.nazomocambique.controller;
import java.security.Principal;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.example.nazomocambique.service.*;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.ConfiguracaoCashbackRepository;
@Controller
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'USUARIO')")
public class CarteiraController {
 private final CarteiraService carteira; private final ContaService conta; private final ConfiguracaoCashbackRepository cashbackRepo;
 public CarteiraController(CarteiraService c, ContaService a, ConfiguracaoCashbackRepository r){carteira=c;conta=a;cashbackRepo=r;}
 @GetMapping("/carteira") public String carteira(Principal p, Authentication auth, Model m){
  boolean admin = auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
  boolean cashbackAtivo = cashbackRepo.findById(1L).map(c -> c.isAtivo()).orElse(false);
  if (!admin && !cashbackAtivo) return "redirect:/home";
  Usuario u=conta.buscarPorUsername(p.getName()).orElseThrow();
  m.addAttribute("saldo",carteira.saldo(u.getId()));m.addAttribute("movimentos",carteira.historico(u.getId()));return "carteira";
 }
}
