package com.laboratorio.financas.fatura.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaturaJpaRepository extends JpaRepository<FaturaEntity, UUID> {

    List<FaturaEntity> findByUserId(UUID userId);
}
