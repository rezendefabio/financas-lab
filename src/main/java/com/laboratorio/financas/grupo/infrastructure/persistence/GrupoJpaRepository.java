package com.laboratorio.financas.grupo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoJpaRepository extends JpaRepository<GrupoEntity, UUID> {

    List<GrupoEntity> findByUserId(UUID userId);

    Optional<GrupoEntity> findByIdAndUserId(UUID id, UUID userId);
}
