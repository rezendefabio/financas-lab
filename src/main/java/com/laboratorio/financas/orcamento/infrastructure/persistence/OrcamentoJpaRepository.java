package com.laboratorio.financas.orcamento.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoJpaRepository extends JpaRepository<OrcamentoEntity, UUID> {
}
