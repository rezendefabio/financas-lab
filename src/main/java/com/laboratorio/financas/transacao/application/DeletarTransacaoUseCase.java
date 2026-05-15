package com.laboratorio.financas.transacao.application;

import com.laboratorio.financas.transacao.domain.TransacaoNaoEncontradaException;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarTransacaoUseCase {

    private final TransacaoRepository repository;

    public DeletarTransacaoUseCase(TransacaoRepository repository) {
        this.repository = repository;
    }

    /**
     * Soft delete: define deleted_at sem remover fisicamente do banco.
     * Transacoes deletadas ficam invisiveis para todas as queries padrao.
     */
    @Transactional
    public void executar(UUID id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new TransacaoNaoEncontradaException(id);
        }
        repository.softDelete(id);
    }
}
