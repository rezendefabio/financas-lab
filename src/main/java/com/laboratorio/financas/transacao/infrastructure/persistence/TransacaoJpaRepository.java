package com.laboratorio.financas.transacao.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {
    // Metodos derivados serao adicionados na 3.7 conforme use cases pedirem.
}
