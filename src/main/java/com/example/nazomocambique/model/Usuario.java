package com.example.nazomocambique.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Transient;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String username;

	private String senha; // hash BCrypt, quando existir login

	private String role; // "ROLE_ADMIN", "ROLE_GESTOR" (só a separação de pedidos), "ROLE_USUARIO"

	private String nome;

	// Dados pessoais (tela de perfil). CPF guardado só com os 11 dígitos; um CPF por conta.
	@Column(unique = true, length = 11)
	private String cpf;

	private LocalDate dataNascimento;

	@Column(nullable = false)
	private boolean ativo = true;

	public void setId(Long id) {
		this.id =id;
	}
	
	public Long getId() {
		return id;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getSenha() {
		return senha;
	}
	
	public void setSenha(String senha) {
		this.senha = senha;
	}
	
	public String getRole() {
		return role;
	}
	
	public void setRole(String role) {
		this.role = role;
	}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	/** CPF no formato 000.000.000-00; null quando não informado. */
	@Transient
	public String getCpfFormatado() {
		if (cpf == null || cpf.length() != 11) {
			return cpf;
		}
		return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
	}

	/** CPF com os dados parcialmente ocultos para exibição: ***.456.789-**; null quando não informado. */
	@Transient
	public String getCpfMascarado() {
		if (cpf == null || cpf.length() != 11) {
			return null;
		}
		return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
	}

	/** Nome para exibir nas telas do admin: o nome, ou o e-mail (login) quando a conta não tem nome. */
	@Transient
	public String getNomeExibicao() {
		return nome != null && !nome.isBlank() ? nome : username;
	}

	/** Perfil em português: Administrador, Gestor ou Cliente. */
	@Transient
	public String getRotuloPerfil() {
		if ("ROLE_ADMIN".equals(role)) {
			return "Administrador";
		}
		if ("ROLE_GESTOR".equals(role)) {
			return "Gestor";
		}
		return "Cliente";
	}

	/** Data de nascimento no formato dd/MM/yyyy; null quando não informada. */
	@Transient
	public String getDataNascimentoFormatada() {
		return dataNascimento == null ? null : dataNascimento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
	}

	public boolean getAtivo() {
		return ativo;
	}
	
	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
}