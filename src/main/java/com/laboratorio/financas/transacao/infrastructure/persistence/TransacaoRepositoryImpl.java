package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Lista com filtros. Filtra deleted_at IS NULL por padrao.
     */
    @Override
    public Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable) {
        return jpaRepository.findComFiltros(
                filtros.contaId(),
                filtros.dataInicio(),
                filtros.dataFim(),
                filtros.tipo(),
                filtros.categoriaId(),
                filtros.userId(),
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
