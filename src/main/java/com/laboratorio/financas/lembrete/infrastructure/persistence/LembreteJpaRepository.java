package com.laboratorio.financas.lembrete.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LembreteJpaRepository extends JpaRepository<LembreteEntity, UUID> {
}
