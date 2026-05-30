package com.laboratorio.financas.lembrete.application;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteNaoEncontradoException;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BuscarLembretePorIdUseCase {

    private final LembreteRepository repository;

    public BuscarLembretePorIdUseCase(LembreteRepository repository) {
        this.repository = repository;
    }

    public Lembrete executar(UUID id, UUID userId) {
        Lembrete lembrete = repository.buscarPorId(id)
                .orElseThrow(() -> new LembreteNaoEncontradoException(id));
        if (!lembrete.getUserId().equals(userId)) {
            throw new LembreteNaoEncontradoException(id);
        }
        return lembrete;
    }
}
