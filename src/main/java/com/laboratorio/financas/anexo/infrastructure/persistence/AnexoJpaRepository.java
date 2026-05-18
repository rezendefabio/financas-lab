package com.laboratorio.financas.anexo.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnexoJpaRepository extends JpaRepository<AnexoEntity, UUID> {
}
