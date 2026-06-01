package com.laboratorio.financas.fatura.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaturaJpaRepository extends JpaRepository<FaturaEntity, UUID> {
}
