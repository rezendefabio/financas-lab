package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {

    /** Busca por id excluindo soft-deleted. */
    @Query("SELECT t FROM TransacaoEntity t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<TransacaoEntity> findByIdAndNotDeleted(@Param("id") UUID id);

    /** Marca deletedAt na transacao sem remover do banco. */
    @Transactional
    @Modifying
    @Query("UPDATE TransacaoEntity t SET t.deletedAt = CURRENT_TIMESTAMP, t.atualizadoEm = CURRENT_TIMESTAMP WHERE t.id = :id")
    void softDeleteById(@Param("id") UUID id);

    /** Lista com filtros, excluindo soft-deleted. userId null significa sem filtro por usuario. */
    @Query("""
            SELECT t FROM TransacaoEntity t
            WHERE (:contaId IS NULL OR t.contaId = :contaId)
              AND t.data >= COALESCE(:dataInicio, t.data)
              AND t.data <= COALESCE(:dataFim, t.data)
              AND (:tipo IS NULL OR t.tipo = :tipo)
              AND (:categoriaId IS NULL OR t.categoriaId = :categoriaId)
              AND (:userId IS NULL OR t.userId = :userId)
              AND (:status IS NULL OR t.status = :status)
              AND t.deletedAt IS NULL
            """)
    Page<TransacaoEntity> findComFiltros(
            @Param("contaId") UUID contaId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("tipo") TipoTransacao tipo,
            @Param("categoriaId") UUID categoriaId,
            @Param("userId") UUID userId,
            @Param("status") StatusTransacao status,
            Pageable pageable
    );

    /** Busca par de transferencia pelo groupId, excluindo soft-deleted. */
    @Query("SELECT t FROM TransacaoEntity t WHERE t.transferGroupId = :groupId AND t.deletedAt IS NULL")
    List<TransacaoEntity> findByTransferGroupId(@Param("groupId") UUID groupId);

    @Query("""
            SELECT new com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta(
                COALESCE(SUM(CASE
                    WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA
                         AND t.transferGroupId IS NULL
                    THEN t.valor.valor ELSE 0 END), 0),
                COALESCE(SUM(CASE
                    WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.DESPESA
                         AND t.transferGroupId IS NULL
                    THEN t.valor.valor ELSE 0 END), 0),
                COALESCE(SUM(CASE
                    WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.DESPESA
                         AND t.transferGroupId IS NOT NULL
                    THEN t.valor.valor ELSE 0 END), 0),
                COALESCE(SUM(CASE
                    WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA
                         AND t.transferGroupId IS NOT NULL
                    THEN t.valor.valor ELSE 0 END), 0)
            )
            FROM TransacaoEntity t
            WHERE t.contaId = :contaId AND t.deletedAt IS NULL
            """)
    TotaisTransacaoPorConta calcularTotaisPorConta(@Param("contaId") UUID contaId);
}
