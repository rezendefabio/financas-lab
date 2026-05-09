package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarContaPorIdUseCase {

    private final ContaRepository repository;

    public BuscarContaPorIdUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Conta executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
    }
}
