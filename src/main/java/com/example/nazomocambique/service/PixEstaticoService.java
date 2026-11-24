package com.example.nazomocambique.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.nazomocambique.config.PixProperties;
import com.example.nazomocambique.model.Pedido;

/**
 * Pix estático do pedido: código "copia e cola" e QR Code com o valor TOTAL do pedido, gerados aqui
 * mesmo, sem gateway. O identificador do pedido (txid) vai dentro do código para a equipe conferir o
 * pagamento no extrato. Este Pix não avisa a loja quando é pago: a equipe confirma o pedido à mão.
 *
 * Só é gerado para pedido Pix que ainda aguarda pagamento.
 */
@Service
public class PixEstaticoService {

	private static final Logger log = LoggerFactory.getLogger(PixEstaticoService.class);
	/** Zona de silêncio do QR Code, em módulos (o padrão pede 4). */
	private static final int BORDA = 4;

	/** Pix pronto para a tela: valor cobrado, código copia e cola e a imagem do QR Code (data URI em SVG). */
	public record PixEstatico(BigDecimal valor, String copiaECola, String qrDataUri) {
	}

	private final PixProperties props;
	private final PixConfiguracaoService configuracoes;

	@org.springframework.beans.factory.annotation.Autowired
	public PixEstaticoService(PixProperties props, PixConfiguracaoService configuracoes) {
		this.props = props;
		this.configuracoes = configuracoes;
	}

	/** Construtor mantido para compatibilidade com testes e integrações existentes. */
	public PixEstaticoService(PixProperties props) {
		this.props = props;
		this.configuracoes = null;
	}

	private PixProperties dadosRecebedor() {
		return configuracoes == null ? props : configuracoes.obter(props);
	}

	public boolean configurado() {
		return dadosRecebedor().configurado();
	}

	/** Vazio se o recebedor não está configurado, o pedido não espera Pix ou o total não é cobrável. */
	public Optional<PixEstatico> gerar(Pedido pedido) {
		PixProperties recebedor = dadosRecebedor();
		if (!recebedor.configurado() || pedido == null || !pedido.isAguardandoPix()) {
			return Optional.empty();
		}
		BigDecimal total = pedido.getTotal();
		try {
			String codigo = PixBrCode.gerar(recebedor.chave(), recebedor.nome(), recebedor.cidade(), total, pedido.getCodigo());
			return Optional.of(new PixEstatico(total, codigo, QrCode.codificar(codigo).paraDataUri(BORDA)));
		} catch (IllegalArgumentException e) {
			// Configuração ou valor inválido: a tela do pedido segue sem o QR Code
			log.warn("Pix estático não gerado para o pedido {}: {}", pedido.getCodigo(), e.getMessage());
			return Optional.empty();
		}
	}
}
