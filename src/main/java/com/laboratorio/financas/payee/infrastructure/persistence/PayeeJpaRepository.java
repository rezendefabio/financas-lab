package com.laboratorio.financas.payee.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayeeJpaRepository extends JpaRepository<PayeeEntity, UUID> {

    List<PayeeEntity> findByUserId(UUID userId);

    Optional<PayeeEntity> findByIdAndUserId(UUID id, UUID userId);
}
