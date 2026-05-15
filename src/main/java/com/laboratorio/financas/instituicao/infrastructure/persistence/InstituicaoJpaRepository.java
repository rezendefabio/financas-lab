package com.laboratorio.financas.instituicao.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoJpaRepository extends JpaRepository<InstituicaoEntity, UUID> {

    List<InstituicaoEntity> findByAtivaTrue();
}
