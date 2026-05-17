package com.laboratorio.financas.incidente.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErroRegistradoJpaRepository extends JpaRepository<ErroRegistradoEntity, UUID> {

    Optional<ErroRegistradoEntity> findByCodigo(String codigo);
}
