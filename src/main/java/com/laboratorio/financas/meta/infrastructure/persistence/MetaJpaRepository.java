package com.laboratorio.financas.meta.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaJpaRepository extends JpaRepository<MetaEntity, UUID> {
}
