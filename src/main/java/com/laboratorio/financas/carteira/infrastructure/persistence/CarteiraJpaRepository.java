package com.laboratorio.financas.carteira.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarteiraJpaRepository extends JpaRepository<CarteiraEntity, UUID> {
}
