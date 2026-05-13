package com.laboratorio.financas.lancamentorecorrente.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRecorrenteJpaRepository
        extends JpaRepository<LancamentoRecorrenteEntity, UUID> {
}
