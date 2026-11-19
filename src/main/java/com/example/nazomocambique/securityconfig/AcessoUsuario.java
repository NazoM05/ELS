package com.example.nazomocambique.securityconfig;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.nazomocambique.model.Usuario;

public class AcessoUsuario implements UserDetails {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Usuario usuario;
	

	public AcessoUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(usuario.getRole()));
	}

	@Override
	public @Nullable String getPassword() {
		return usuario.getSenha();
	}

	@Override
	public String getUsername() {
		return usuario.getUsername();
	}


}
