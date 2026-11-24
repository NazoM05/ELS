package com.example.nazomocambique.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga a leitura das propriedades das integrações externas: "pix.*", "asaas.*" e "correios.*". */
@Configuration
@EnableConfigurationProperties({ PixProperties.class, AsaasProperties.class, CorreiosProperties.class })
public class IntegracoesConfig {
}
