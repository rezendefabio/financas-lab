package com.laboratorio.financas.lembrete.application;

import com.laboratorio.financas.lembrete.domain.LembreteNaoEncontradoException;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarLembreteUseCase {

    private final LembreteRepository repository;

    public DeletarLembreteUseCase(LembreteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new LembreteNaoEncontradoException(id));
        repository.deletar(id);
    }
}
