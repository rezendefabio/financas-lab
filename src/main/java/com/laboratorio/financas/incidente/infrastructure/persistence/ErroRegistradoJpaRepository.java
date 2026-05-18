package com.laboratorio.financas.incidente.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ErroRegistradoJpaRepository extends JpaRepository<ErroRegistradoEntity, UUID> {

    Optional<ErroRegistradoEntity> findByCodigo(String codigo);

    @Query("""
            SELECT e FROM ErroRegistradoEntity e
            WHERE e.criadoEm >= COALESCE(:criadoApartirDe, e.criadoEm)
              AND e.criadoEm <= COALESCE(:criadoAte, e.criadoEm)
              AND (CAST(:classeErro AS string) IS NULL
                   OR LOWER(e.classeErro) LIKE LOWER(CONCAT('%', CAST(:classeErro AS string), '%')))
              AND (CAST(:operacao AS string) IS NULL
                   OR LOWER(e.operacao) LIKE LOWER(CONCAT('%', CAST(:operacao AS string), '%')))
            ORDER BY e.criadoEm DESC
            """)
    List<ErroRegistradoEntity> findComFiltros(
            @Param("criadoApartirDe") Instant criadoApartirDe,
            @Param("criadoAte") Instant criadoAte,
            @Param("classeErro") String classeErro,
            @Param("operacao") String operacao);
}
