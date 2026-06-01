package com.laboratorio.financas.limite.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimiteJpaRepository extends JpaRepository<LimiteEntity, UUID> {
}
