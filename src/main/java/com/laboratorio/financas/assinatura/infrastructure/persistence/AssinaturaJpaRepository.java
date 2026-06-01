package com.laboratorio.financas.assinatura.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaJpaRepository extends JpaRepository<AssinaturaEntity, UUID> {
}
