package com.laboratorio.financas.transacao.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
     * Lista transacoes com filtros, sem ordenacao explicita por campo de dominio.
     * Filtra deleted_at IS NULL por padrao.
     *
     * <p>Usada por consumidores que so precisam de filtragem/paginacao
     * (ex: relatorios com {@code Pageable.unpaged()}).
     */
    Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable);

    /**
     * Lista transacoes com filtros, paginadas e ordenadas por um campo de
     * dominio. Filtra deleted_at IS NULL por padrao.
     *
     * <p>A ordenacao e expressa por um campo de dominio ({@link OrdenacaoTransacao})
     * e uma direcao. A traducao do campo de dominio para o caminho de propriedade
     * de persistencia e responsabilidade da camada de infraestrutura.
     *
     * @param filtros  criterios de filtragem
     * @param page     indice da pagina (base zero)
     * @param size     tamanho da pagina
     * @param ordenacao campo de dominio pelo qual ordenar
     * @param direcao  direcao da ordenacao
     */
    Page<Transacao> listarComFiltrosOrdenado(
            FiltrosTransacao filtros,
            int page,
            int size,
            OrdenacaoTransacao ordenacao,
            Sort.Direction direcao);

    /**
     * Busca todas as transacoes de um grupo de transferencia (par DESPESA+RECEITA).
     * Filtra deleted_at IS NULL por padrao.
     */
    List<Transacao> findByTransferGroupId(UUID groupId);

    TotaisTransacaoPorConta calcularTotaisPorConta(UUID contaId);
}
