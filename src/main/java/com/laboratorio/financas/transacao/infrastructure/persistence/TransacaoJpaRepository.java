package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
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
              AND t.data >= COALESCE(:dataInicio, t.data)
              AND t.data <= COALESCE(:dataFim, t.data)
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

    @Query("""
            SELECT new com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta(
                COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA
                        AND t.contaId = :contaId THEN t.valor.valor ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.DESPESA
                        AND t.contaId = :contaId THEN t.valor.valor ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.TRANSFERENCIA
                        AND t.contaId = :contaId THEN t.valor.valor ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.TRANSFERENCIA
                        AND t.contaDestinoId = :contaId THEN t.valor.valor ELSE 0 END), 0)
            )
            FROM TransacaoEntity t
            WHERE t.contaId = :contaId OR t.contaDestinoId = :contaId
            """)
    TotaisTransacaoPorConta calcularTotaisPorConta(@Param("contaId") UUID contaId);
}
