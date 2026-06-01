package com.laboratorio.financas.emprestimo.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmprestimoJpaRepository extends JpaRepository<EmprestimoEntity, UUID> {
}
