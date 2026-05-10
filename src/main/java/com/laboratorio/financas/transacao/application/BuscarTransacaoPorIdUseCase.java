package com.laboratorio.financas.transacao.application;

import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoNaoEncontradaException;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarTransacaoPorIdUseCase {

    private final TransacaoRepository repository;

    public BuscarTransacaoPorIdUseCase(TransacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Transacao executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new TransacaoNaoEncontradaException(id));
    }
}
