package com.laboratorio.financas.tag.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagJpaRepository extends JpaRepository<TagEntity, UUID> {

    List<TagEntity> findByUserId(UUID userId);

    Optional<TagEntity> findByIdAndUserId(UUID id, UUID userId);
}
