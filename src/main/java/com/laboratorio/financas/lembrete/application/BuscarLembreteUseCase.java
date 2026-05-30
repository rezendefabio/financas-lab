package com.laboratorio.financas.lembrete.application;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteNaoEncontradoException;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarLembreteUseCase {

    private final LembreteRepository repository;

    public BuscarLembreteUseCase(LembreteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Lembrete executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new LembreteNaoEncontradoException(id));
    }
}
