package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TransacaoJpaRepository
        extends JpaRepository<TransacaoEntity, UUID>, JpaSpecificationExecutor<TransacaoEntity> {

    /** Busca por id excluindo soft-deleted. */
    @Query("SELECT t FROM TransacaoEntity t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<TransacaoEntity> findByIdAndNotDeleted(@Param("id") UUID id);

    /** Marca deletedAt na transacao sem remover do banco. */
    @Transactional
    @Modifying
    @Query("UPDATE TransacaoEntity t SET t.deletedAt = CURRENT_TIMESTAMP, t.atualizadoEm = CURRENT_TIMESTAMP WHERE t.id = :id")
    void softDeleteById(@Param("id") UUID id);

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
