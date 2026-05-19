package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.OrdenacaoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class TransacaoRepositoryImpl implements TransacaoRepository {

    private final TransacaoJpaRepository jpaRepository;
    private final TransacaoMapper mapper;

    public TransacaoRepositoryImpl(TransacaoJpaRepository jpaRepository, TransacaoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Transacao salvar(Transacao transacao) {
        TransacaoEntity entity = mapper.toEntity(transacao);
        TransacaoEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    /**
     * Busca por id filtrando deleted_at IS NULL.
     */
    @Override
    public Optional<Transacao> buscarPorId(UUID id) {
        return jpaRepository.findByIdAndNotDeleted(id).map(mapper::toDomain);
    }

    /**
     * Soft delete: define deleted_at sem remover fisicamente do banco.
     */
    @Override
    public void softDelete(UUID id) {
        jpaRepository.softDeleteById(id);
    }

    /**
     * Delete fisico. Disponivel para uso em testes; em producao usar softDelete.
     */
    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }

    /**
     * Lista com filtros sem ordenacao por campo de dominio. Filtra
     * deleted_at IS NULL por padrao.
     */
    @Override
    public Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable) {
        return executarFiltros(filtros, pageable);
    }

    /**
     * Lista com filtros e ordenacao. Filtra deleted_at IS NULL por padrao.
     *
     * <p>Constroi o {@link Pageable} traduzindo o campo de dominio
     * {@link OrdenacaoTransacao} no caminho de propriedade JPA -- detalhe de
     * persistencia que so a infraestrutura conhece.
     */
    @Override
    public Page<Transacao> listarComFiltrosOrdenado(
            FiltrosTransacao filtros,
            int page,
            int size,
            OrdenacaoTransacao ordenacao,
            Sort.Direction direcao) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direcao, OrdenacaoTransacaoJpaPath.resolver(ordenacao)));
        return executarFiltros(filtros, pageable);
    }

    private Page<Transacao> executarFiltros(FiltrosTransacao filtros, Pageable pageable) {
        return jpaRepository.findComFiltros(
                filtros.contaId(),
                filtros.dataInicio(),
                filtros.dataFim(),
                filtros.tipo(),
                filtros.categoriaId(),
                filtros.userId(),
                filtros.status(),
                pageable
        ).map(mapper::toDomain);
    }

    /**
     * Busca par de transferencia pelo groupId. Filtra deleted_at IS NULL.
     */
    @Override
    public List<Transacao> findByTransferGroupId(UUID groupId) {
        return jpaRepository.findByTransferGroupId(groupId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public TotaisTransacaoPorConta calcularTotaisPorConta(UUID contaId) {
        return jpaRepository.calcularTotaisPorConta(contaId);
    }
}
