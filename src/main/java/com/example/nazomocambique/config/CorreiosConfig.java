package com.example.nazomocambique.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga a leitura das propriedades "correios.*". */
@Configuration
@EnableConfigurationProperties(CorreiosProperties.class)
public class CorreiosConfig {
}
