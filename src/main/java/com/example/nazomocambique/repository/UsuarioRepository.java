package com.example.nazomocambique.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.nazomocambique.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

	Optional<Usuario> findByUsername(String username);

	// O e-mail da conta é o username; a checagem ignora maiúsculas/minúsculas para não duplicar
	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

	boolean existsByCpfAndIdNot(String cpf, Long id);

}
