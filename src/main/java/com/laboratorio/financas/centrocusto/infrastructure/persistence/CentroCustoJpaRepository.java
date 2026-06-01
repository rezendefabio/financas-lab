package com.laboratorio.financas.centrocusto.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CentroCustoJpaRepository extends JpaRepository<CentroCustoEntity, UUID> {

    Optional<CentroCustoEntity> findByIdAndUserId(UUID id, UUID userId);
}
