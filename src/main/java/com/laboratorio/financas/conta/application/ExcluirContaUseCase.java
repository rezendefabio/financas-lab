package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExcluirContaUseCase {

    private final ContaRepository repository;

    public ExcluirContaUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
        repository.deletar(id);
    }
}
