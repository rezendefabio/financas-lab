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

    @Transactional
    public void executar(UUID id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new TransacaoNaoEncontradaException(id);
        }
        repository.deletar(id);
    }
}
