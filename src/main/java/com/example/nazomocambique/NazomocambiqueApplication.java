package com.example.nazomocambique;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NazomocambiqueApplication {

	public static void main(String[] args) {
		SpringApplication.run(NazomocambiqueApplication.class, args);
	}

}
