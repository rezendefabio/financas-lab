package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {

    @Query("""
            SELECT t FROM TransacaoEntity t
            WHERE (:contaId IS NULL OR t.contaId = :contaId OR t.contaDestinoId = :contaId)
              AND t.data >= :dataInicio
              AND t.data <= :dataFim
              AND (:tipo IS NULL OR t.tipo = :tipo)
              AND (:categoriaId IS NULL OR t.categoriaId = :categoriaId)
            """)
    Page<TransacaoEntity> findComFiltros(
            @Param("contaId") UUID contaId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("tipo") TipoTransacao tipo,
            @Param("categoriaId") UUID categoriaId,
            Pageable pageable
    );
}
