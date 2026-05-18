package com.laboratorio.financas.anexo.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnexoJpaRepository extends JpaRepository<AnexoEntity, UUID> {

    List<AnexoEntity> findByEntidadeTipoAndEntidadeId(String entidadeTipo, UUID entidadeId);
}
