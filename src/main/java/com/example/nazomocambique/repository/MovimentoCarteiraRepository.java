package com.example.nazomocambique.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.nazomocambique.model.MovimentoCarteira;
public interface MovimentoCarteiraRepository extends JpaRepository<MovimentoCarteira,Long> {
 List<MovimentoCarteira> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
 Optional<MovimentoCarteira> findByReferencia(String referencia);
}
