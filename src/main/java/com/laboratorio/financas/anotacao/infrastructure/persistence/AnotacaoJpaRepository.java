package com.laboratorio.financas.anotacao.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnotacaoJpaRepository extends JpaRepository<AnotacaoEntity, UUID> {

    List<AnotacaoEntity> findByUserId(UUID userId);
}
