package com.laboratorio.financas.grupo.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoJpaRepository extends JpaRepository<GrupoEntity, UUID> {

    Optional<GrupoEntity> findByIdAndUserId(UUID id, UUID userId);
}
