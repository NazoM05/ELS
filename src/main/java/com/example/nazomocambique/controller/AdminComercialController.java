package com.example.nazomocambique.controller;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nazomocambique.model.ConfiguracaoCashback;
import com.example.nazomocambique.repository.ConfiguracaoCashbackRepository;
import com.example.nazomocambique.service.PixConfiguracaoService;

/**
 * Configuração comercial da loja (só ADMIN, pela regra de /admin/** no SecurityConfig): dados do recebedor
 * Pix e regras de cashback. A central fica em /admin/comercial; as telas separadas /admin/pix e
 * /admin/cashback continuam disponíveis, cada uma com os mesmos textos e redirecionamentos de antes.
 */
@Controller
public class AdminComercialController {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final PixConfiguracaoService pix;
    private final ConfiguracaoCashbackRepository cashbackRepo;

    public AdminComercialController(PixConfiguracaoService pix, ConfiguracaoCashbackRepository cashbackRepo) {
        this.pix = pix;
        this.cashbackRepo = cashbackRepo;
    }

    // ---------------------------------------------------------------- central (/admin/comercial)

    @GetMapping("/admin/comercial")
    public String tela(Model model) {
        model.addAttribute("pix", pix.atual());
        model.addAttribute("cashback", configuracaoCashback());
        return "admin/comercial";
    }

    @PostMapping("/admin/comercial/pix")
    public String salvarPix(@RequestParam String chave, @RequestParam String nome,
            @RequestParam String cidade, RedirectAttributes redirect) {
        gravarPix(chave, nome, cidade, "Dados do Pix salvos com sucesso.", redirect);
        return "redirect:/admin/comercial#pix";
    }

    @PostMapping("/admin/comercial/cashback")
    public String salvarCashback(@RequestParam(defaultValue = "false") boolean ativo,
            @RequestParam BigDecimal percentual, @RequestParam BigDecimal minimoCompra,
            @RequestParam BigDecimal limitePorCompra, RedirectAttributes redirect) {
        gravarCashback(ativo, percentual, minimoCompra, limitePorCompra, "Configuração de cashback salva com sucesso.",
                redirect);
        return "redirect:/admin/comercial#cashback";
    }

    // ---------------------------------------------------------------- tela do Pix (/admin/pix)

    @GetMapping("/admin/pix")
    public String telaPix(Model model) {
        var dados = pix.atual();
        model.addAttribute("chave", dados.chave());
        model.addAttribute("nome", dados.nome());
        model.addAttribute("cidade", dados.cidade());
        return "admin/pix";
    }

    @PostMapping("/admin/pix")
    public String salvarPixTela(@RequestParam String chave, @RequestParam String nome,
            @RequestParam String cidade, RedirectAttributes redirect) {
        gravarPix(chave, nome, cidade, "Dados do Pix salvos. Os próximos QR Codes usarão esta chave.", redirect);
        return "redirect:/admin/pix";
    }

    // ---------------------------------------------------------------- tela do cashback (/admin/cashback)

    @GetMapping("/admin/cashback")
    public String telaCashback(Model model) {
        model.addAttribute("config", configuracaoCashback());
        return "admin/cashback";
    }

    @PostMapping("/admin/cashback")
    public String salvarCashbackTela(@RequestParam(defaultValue = "false") boolean ativo,
            @RequestParam BigDecimal percentual, @RequestParam BigDecimal minimoCompra,
            @RequestParam BigDecimal limitePorCompra, RedirectAttributes redirect) {
        gravarCashback(ativo, percentual, minimoCompra, limitePorCompra, "Configuração de cashback salva.", redirect);
        return "redirect:/admin/cashback";
    }

    // ---------------------------------------------------------------- regras compartilhadas

    private void gravarPix(String chave, String nome, String cidade, String mensagemSucesso,
            RedirectAttributes redirect) {
        try {
            pix.salvar(chave, nome, cidade);
            redirect.addFlashAttribute("sucesso", mensagemSucesso);
        } catch (IllegalArgumentException | IOException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
    }

    private void gravarCashback(boolean ativo, BigDecimal percentual, BigDecimal minimoCompra,
            BigDecimal limitePorCompra, String mensagemSucesso, RedirectAttributes redirect) {
        if (percentual.signum() < 0 || percentual.compareTo(CEM) > 0
                || minimoCompra.signum() < 0 || limitePorCompra.signum() < 0) {
            redirect.addFlashAttribute("erro", "Informe valores válidos. O percentual deve estar entre 0 e 100.");
            return;
        }
        ConfiguracaoCashback c = configuracaoCashback();
        c.setAtivo(ativo);
        c.setPercentual(percentual);
        c.setMinimoCompra(minimoCompra);
        c.setLimitePorCompra(limitePorCompra);
        cashbackRepo.save(c);
        redirect.addFlashAttribute("sucesso", mensagemSucesso);
    }

    private ConfiguracaoCashback configuracaoCashback() {
        return cashbackRepo.findById(1L).orElseGet(ConfiguracaoCashback::new);
    }
}
