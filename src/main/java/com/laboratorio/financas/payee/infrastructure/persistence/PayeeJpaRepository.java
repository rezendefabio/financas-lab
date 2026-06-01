package com.laboratorio.financas.payee.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayeeJpaRepository extends JpaRepository<PayeeEntity, UUID> {

    Optional<PayeeEntity> findByIdAndUserId(UUID id, UUID userId);
}
