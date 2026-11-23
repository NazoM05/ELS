package com.example.nazomocambique.config;

import java.nio.file.Path;
import java.time.Duration;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.nazomocambique.service.AparenciaService;

/**
 * Serve /img/** a partir de dois lugares:
 *  1) uploads/img  -> imagens enviadas pelo admin (frente e costas dos produtos);
 *  2) static/img   -> imagens que já vêm com o projeto (ex.: camisa-militar.svg).
 * Sem isso, as imagens enviadas ficam em disco mas a loja não consegue exibi-las.
 */
@Configuration
public class ImagensWebConfig implements WebMvcConfigurer {

	// Mesma pasta usada pelo AdminController para gravar os uploads
	private static final Path PASTA_UPLOADS = Paths.get("uploads/img");

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String uploads = PASTA_UPLOADS.toAbsolutePath().toUri().toString();
		if (!uploads.endsWith("/")) {
			uploads += "/"; // a pasta pode ainda não existir; o Spring exige a barra final
		}
		registry.addResourceHandler("/img/**")
				.addResourceLocations(uploads, "classpath:/static/img/")
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());

		// Fontes locais, caso sejam adicionadas ao projeto, também podem ficar em cache longo.
		registry.addResourceHandler("/fonts/**")
				.addResourceLocations("classpath:/static/fonts/")
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());

		// Logo enviada na tela Configurações (uploads/logo). Os nomes são UUID, então o cache longo é seguro.
		String logos = AparenciaService.PASTA_LOGO.toAbsolutePath().toUri().toString();
		if (!logos.endsWith("/")) {
			logos += "/";
		}
		registry.addResourceHandler("/logo-site/**")
				.addResourceLocations(logos)
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
	}
}
