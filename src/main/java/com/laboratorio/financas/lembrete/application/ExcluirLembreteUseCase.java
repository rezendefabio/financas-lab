package com.laboratorio.financas.lembrete.application;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteNaoEncontradoException;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExcluirLembreteUseCase {

    private final LembreteRepository repository;

    public ExcluirLembreteUseCase(LembreteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id, UUID userId) {
        Lembrete lembrete = repository.buscarPorId(id)
                .orElseThrow(() -> new LembreteNaoEncontradoException(id));
        if (!lembrete.getUserId().equals(userId)) {
            throw new LembreteNaoEncontradoException(id);
        }
        repository.deletar(id);
    }
}
