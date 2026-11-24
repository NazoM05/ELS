package com.example.nazomocambique.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.nazomocambique.model.CodigoRedefinicaoSenha;
import com.example.nazomocambique.model.Usuario;

public interface CodigoRedefinicaoSenhaRepository extends JpaRepository<CodigoRedefinicaoSenha, Long> {
    Optional<CodigoRedefinicaoSenha> findByUsuario(Usuario usuario);
    void deleteByUsuario(Usuario usuario);
}
