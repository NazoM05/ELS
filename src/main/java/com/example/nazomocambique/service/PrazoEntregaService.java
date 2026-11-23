package com.example.nazomocambique.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.nazomocambique.model.PrazoEntrega;

/**
 * Prazos de entrega por estado (dias úteis após o envio).
 * Para mudar um prazo, altere só a linha do estado aqui: o mapa da página Frete e Entrega lê daqui.
 */
@Service
public class PrazoEntregaService {

	private static final Map<String, PrazoEntrega> PRAZOS = new LinkedHashMap<>();

	static {
		// Sul
		add("PR", "Paraná", "Sul", 2, 4);
		add("SC", "Santa Catarina", "Sul", 2, 4);
		add("RS", "Rio Grande do Sul", "Sul", 3, 5);
		// Sudeste
		add("SP", "São Paulo", "Sudeste", 3, 5);
		add("RJ", "Rio de Janeiro", "Sudeste", 4, 6);
		add("MG", "Minas Gerais", "Sudeste", 4, 6);
		add("ES", "Espírito Santo", "Sudeste", 5, 7);
		// Centro-Oeste
		add("MS", "Mato Grosso do Sul", "Centro-Oeste", 4, 6);
		add("GO", "Goiás", "Centro-Oeste", 5, 7);
		add("DF", "Distrito Federal", "Centro-Oeste", 5, 7);
		add("MT", "Mato Grosso", "Centro-Oeste", 6, 8);
		// Nordeste
		add("BA", "Bahia", "Nordeste", 6, 9);
		add("SE", "Sergipe", "Nordeste", 7, 10);
		add("AL", "Alagoas", "Nordeste", 7, 10);
		add("PE", "Pernambuco", "Nordeste", 7, 10);
		add("PB", "Paraíba", "Nordeste", 8, 11);
		add("RN", "Rio Grande do Norte", "Nordeste", 8, 11);
		add("CE", "Ceará", "Nordeste", 8, 11);
		add("PI", "Piauí", "Nordeste", 8, 12);
		add("MA", "Maranhão", "Nordeste", 9, 12);
		// Norte
		add("TO", "Tocantins", "Norte", 7, 10);
		add("RO", "Rondônia", "Norte", 8, 12);
		add("PA", "Pará", "Norte", 9, 13);
		add("AC", "Acre", "Norte", 10, 14);
		add("AM", "Amazonas", "Norte", 10, 15);
		add("AP", "Amapá", "Norte", 11, 15);
		add("RR", "Roraima", "Norte", 11, 15);
	}

	private static void add(String uf, String nome, String regiao, int min, int max) {
		PRAZOS.put(uf, new PrazoEntrega(uf, nome, regiao, min, max));
	}

	/** Todos os estados, agrupados por região (ordem de exibição). */
	public List<PrazoEntrega> listar() {
		return List.copyOf(PRAZOS.values());
	}

	public Optional<PrazoEntrega> buscar(String uf) {
		if (uf == null) return Optional.empty();
		return Optional.ofNullable(PRAZOS.get(uf.trim().toUpperCase()));
	}
}
