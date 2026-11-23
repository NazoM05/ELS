package com.example.nazomocambique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.nazomocambique.model.FormaPagamento;
import com.example.nazomocambique.model.Pedido;
import com.example.nazomocambique.model.StatusPedido;
import com.example.nazomocambique.model.Usuario;
import com.example.nazomocambique.repository.PedidoRepository;
import com.example.nazomocambique.repository.UsuarioRepository;
import com.example.nazomocambique.service.GestaoUsuarioService.DetalheUsuario;
import com.example.nazomocambique.service.GestaoUsuarioService.GestaoUsuarioException;
import com.example.nazomocambique.service.GestaoUsuarioService.ResumoUsuario;

/** Gestão de usuários do admin: totais de compra, ficha com histórico e ativar/desativar com as travas. */
// Banco próprio: o data.sql roda de novo neste contexto (um único administrador: "admin")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:gestao-usuarios")
@Transactional
class GestaoUsuarioServiceTests {

	@Autowired
	private GestaoUsuarioService gestao;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PedidoRepository pedidoRepository;

	private int contadorPedidos = 0;

	private Usuario novoUsuario(String email, String role) {
		Usuario u = new Usuario();
		u.setUsername(email);
		u.setNome("Teste " + email);
		u.setRole(role);
		u.setAtivo(true);
		return usuarioRepository.save(u);
	}

	private Pedido novoPedido(Usuario usuario, StatusPedido status, String total) {
		Pedido p = new Pedido();
		p.setCodigo("EL-TST-" + String.format("%04d", ++contadorPedidos));
		p.setUsuario(usuario);
		p.setStatus(status);
		p.setNomeDestinatario("Fulano de Tal");
		p.setFormaPagamento(FormaPagamento.PIX);
		p.setSubtotal(new BigDecimal(total));
		p.setFrete(BigDecimal.ZERO);
		p.setTotal(new BigDecimal(total));
		return pedidoRepository.save(p);
	}

	private ResumoUsuario linhaDe(String email, String loginAdmin) {
		return gestao.listar(loginAdmin).stream().filter(r -> email.equals(r.getUsuario().getUsername())).findFirst().orElseThrow();
	}

	// ------------------------------------------------------------ lista e ficha

	@Test
	void listaSomaSoPedidosPagosEContaTodosOsPedidos() {
		Usuario cliente = novoUsuario("cliente@teste.com", "ROLE_USUARIO");
		novoPedido(cliente, StatusPedido.PAGO, "100.00");
		novoPedido(cliente, StatusPedido.ENTREGUE, "50.50");
		novoPedido(cliente, StatusPedido.CANCELADO, "999.00");
		novoPedido(cliente, StatusPedido.AGUARDANDO_PAGAMENTO, "30.00");

		ResumoUsuario linha = linhaDe("cliente@teste.com", "admin");

		assertEquals(4, linha.getTotalPedidos());
		assertEquals(0, new BigDecimal("150.50").compareTo(linha.getTotalGasto()));
		assertNotNull(linha.getUltimoPedidoEm());
		assertNotNull(linha.getUltimoPedidoEmFormatado());
	}

	@Test
	void usuarioSemPedidosTemTotaisZerados() {
		novoUsuario("sem-pedidos@teste.com", "ROLE_USUARIO");

		ResumoUsuario linha = linhaDe("sem-pedidos@teste.com", "admin");

		assertEquals(0, linha.getTotalPedidos());
		assertEquals(0, BigDecimal.ZERO.compareTo(linha.getTotalGasto()));
		assertNull(linha.getUltimoPedidoEm());
	}

	@Test
	void fichaTrazHistoricoTicketMedioEContagemPorEtapa() {
		Usuario cliente = novoUsuario("ficha@teste.com", "ROLE_USUARIO");
		novoPedido(cliente, StatusPedido.PAGO, "100.00");
		novoPedido(cliente, StatusPedido.ENTREGUE, "50.50");
		novoPedido(cliente, StatusPedido.CANCELADO, "999.00");

		DetalheUsuario ficha = gestao.detalhar(cliente.getId(), "admin").orElseThrow();

		assertEquals(3, ficha.getTotalPedidos());
		assertEquals(3, ficha.getPedidos().size());
		assertEquals(0, new BigDecimal("150.50").compareTo(ficha.getTotalGasto()));
		assertEquals(0, new BigDecimal("75.25").compareTo(ficha.getTicketMedio()));
		assertEquals(1L, ficha.getContagemPorStatus().get(StatusPedido.CANCELADO));
		assertEquals(1L, ficha.getPedidosEmAberto()); // só o PAGO ainda precisa de ação
		assertNotNull(ficha.getUltimoPedido());
	}

	@Test
	void fichaSemPedidosNaoTemTicketMedio() {
		Usuario cliente = novoUsuario("vazio@teste.com", "ROLE_USUARIO");

		DetalheUsuario ficha = gestao.detalhar(cliente.getId(), "admin").orElseThrow();

		assertEquals(0, ficha.getTotalPedidos());
		assertNull(ficha.getTicketMedio());
		assertNull(ficha.getUltimoPedido());
	}

	@Test
	void fichaDeUsuarioInexistenteVeioVazia() {
		assertTrue(gestao.detalhar(-1L, "admin").isEmpty());
	}

	// ------------------------------------------------------------ ativar e desativar

	@Test
	void desativaEReativaUmaConta() {
		Usuario cliente = novoUsuario("alterna@teste.com", "ROLE_USUARIO");

		assertTrue(gestao.alterarAtivo(cliente.getId(), false, "admin"));
		assertFalse(usuarioRepository.findById(cliente.getId()).orElseThrow().getAtivo());
		assertFalse(gestao.alterarAtivo(cliente.getId(), false, "admin")); // já estava desativada

		assertTrue(gestao.alterarAtivo(cliente.getId(), true, "admin"));
		assertTrue(usuarioRepository.findById(cliente.getId()).orElseThrow().getAtivo());
	}

	@Test
	void adminNaoDesativaAPropriaConta() {
		Usuario outroAdmin = novoUsuario("admin2@teste.com", "ROLE_ADMIN");

		GestaoUsuarioException e = assertThrows(GestaoUsuarioException.class,
				() -> gestao.alterarAtivo(outroAdmin.getId(), false, "admin2@teste.com"));

		assertTrue(e.getMessage().contains("própria conta"));
		assertTrue(usuarioRepository.findById(outroAdmin.getId()).orElseThrow().getAtivo());
	}

	@Test
	void naoDesativaOUltimoAdministradorAtivo() {
		Usuario admin = usuarioRepository.findByUsername("admin").orElseThrow();

		GestaoUsuarioException e = assertThrows(GestaoUsuarioException.class,
				() -> gestao.alterarAtivo(admin.getId(), false, "outro-login"));

		assertTrue(e.getMessage().contains("último administrador"));
		assertTrue(usuarioRepository.findById(admin.getId()).orElseThrow().getAtivo());
	}

	@Test
	void comDoisAdministradoresUmPodeDesativarOOutro() {
		Usuario outroAdmin = novoUsuario("admin3@teste.com", "ROLE_ADMIN");

		assertTrue(gestao.alterarAtivo(outroAdmin.getId(), false, "admin"));
		assertFalse(usuarioRepository.findById(outroAdmin.getId()).orElseThrow().getAtivo());
	}

	@Test
	void gestorPodeSerDesativado() {
		Usuario gestor = novoUsuario("gestor2@teste.com", "ROLE_GESTOR");

		assertTrue(gestao.alterarAtivo(gestor.getId(), false, "admin"));
	}

	@Test
	void usuarioInexistenteGeraErroDeNegocio() {
		assertThrows(GestaoUsuarioException.class, () -> gestao.alterarAtivo(-1L, false, "admin"));
	}

	@Test
	void listaBloqueiaOBotaoDaPropriaContaEDoUltimoAdmin() {
		novoUsuario("livre@teste.com", "ROLE_USUARIO");

		List<ResumoUsuario> linhas = gestao.listar("admin");

		assertNotNull(linhaDe("admin", "admin").getBloqueioDesativar());
		assertNull(linhaDe("livre@teste.com", "admin").getBloqueioDesativar());
		assertEquals(linhas.size(), linhas.stream().map(r -> r.getUsuario().getId()).distinct().count());
	}
}
