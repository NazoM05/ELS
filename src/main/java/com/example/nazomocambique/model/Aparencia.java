package com.example.nazomocambique.model;

/**
 * Identidade visual do site (cor principal + logo), imutável.
 * É exposta a todas as telas como "site" (ver AparenciaAdvice) e só carrega valores já validados.
 */
public class Aparencia {

	public static final String COR_PADRAO = "#0A0A0A";
	private static final String COR_TEXTO_ESCURO = "#0A0A0A";
	private static final String COR_TEXTO_CLARO = "#FFFFFF";

	private final String corPrimaria;   // sempre no formato #RRGGBB (maiúsculas)
	private final String logoArquivo;   // nome do arquivo em uploads/logo, ou null = logo padrão

	public Aparencia(String corPrimaria, String logoArquivo) {
		this.corPrimaria = corPrimaria;
		this.logoArquivo = logoArquivo;
	}

	public static Aparencia padrao() {
		return new Aparencia(COR_PADRAO, null);
	}

	public String getCorPrimaria() {
		return corPrimaria;
	}

	public String getLogoArquivo() {
		return logoArquivo;
	}

	public boolean isLogoPersonalizada() {
		return logoArquivo != null;
	}

	public boolean isPadrao() {
		return COR_PADRAO.equalsIgnoreCase(corPrimaria) && logoArquivo == null;
	}

	/** Cor do texto sobre a cor principal: a de maior contraste (WCAG) entre branco e quase-preto. */
	public String getCorContraste() {
		return contrasteComBranco() >= contrasteComEscuro() ? COR_TEXTO_CLARO : COR_TEXTO_ESCURO;
	}

	/** Só faz sentido para logos em áreas coloridas (menu lateral, painel do login). */
	public boolean isFundoPrimarioEscuro() {
		return COR_TEXTO_CLARO.equals(getCorContraste());
	}

	/**
	 * Bloco de CSS que sobrescreve as variáveis do tema. Só entram valores validados (hex #RRGGBB e
	 * filtros fixos), por isso pode ser impresso sem escape no <style>.
	 */
	public String getCssTema() {
		String filtro;
		if (isLogoPersonalizada()) {
			// Logo enviada: vira uma silhueta de cor única, para ler bem sobre qualquer cor principal
			filtro = isFundoPrimarioEscuro() ? "brightness(0) invert(1)" : "brightness(0)";
		} else {
			// Logo padrão sobre a cor principal é branca; inverte se a cor for clara
			filtro = isFundoPrimarioEscuro() ? "none" : "invert(1)";
		}
		return ":root{--primary-color:" + corPrimaria + ";--primary-contrast:" + getCorContraste()
				+ ";--logo-filtro:" + filtro + ";}";
	}

	private double contrasteComBranco() {
		return 1.05 / (luminancia(corPrimaria) + 0.05);
	}

	private double contrasteComEscuro() {
		return (luminancia(corPrimaria) + 0.05) / (luminancia(COR_TEXTO_ESCURO) + 0.05);
	}

	// Luminância relativa (WCAG 2.x) de uma cor #RRGGBB
	private static double luminancia(String hex) {
		double r = canal(Integer.parseInt(hex.substring(1, 3), 16));
		double g = canal(Integer.parseInt(hex.substring(3, 5), 16));
		double b = canal(Integer.parseInt(hex.substring(5, 7), 16));
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	private static double canal(int valor) {
		double c = valor / 255.0;
		return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}
}
