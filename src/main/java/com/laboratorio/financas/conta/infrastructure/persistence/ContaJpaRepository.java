package com.laboratorio.financas.conta.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaJpaRepository extends JpaRepository<ContaEntity, UUID> {

    List<ContaEntity> findByAtivaTrue();
}
