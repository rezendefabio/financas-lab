package com.laboratorio.financas.transacao.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransacaoRepository {

    Transacao salvar(Transacao transacao);

    /**
     * Busca transacao por id. Retorna vazio se nao existir ou se estiver soft-deleted.
     * Todas as buscas padrao filtram deleted_at IS NULL.
     */
    Optional<Transacao> buscarPorId(UUID id);

    /**
     * Soft delete: define deleted_at para o instante atual. Nao remove o registro do banco.
     */
    void softDelete(UUID id);

    /**
     * Delete fisico. Usado internamente em testes de integracao.
     * Em producao, preferir softDelete.
     */
    void deletar(UUID id);

    /**
     * Lista transacoes com filtros. Filtra deleted_at IS NULL por padrao.
     */
    Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable);

    /**
     * Busca todas as transacoes de um grupo de transferencia (par DESPESA+RECEITA).
     * Filtra deleted_at IS NULL por padrao.
     */
    List<Transacao> findByTransferGroupId(UUID groupId);

    TotaisTransacaoPorConta calcularTotaisPorConta(UUID contaId);
}
