package com.laboratorio.financas.limite.application;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteNaoEncontradoException;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BuscarLimiteUseCase {

    private final LimiteRepository repository;

    public BuscarLimiteUseCase(LimiteRepository repository) {
        this.repository = repository;
    }

    public Limite executar(UUID id, UUID userId) {
        Limite limite = repository.buscarPorId(id)
                .orElseThrow(() -> new LimiteNaoEncontradoException(id));
        if (!limite.getUserId().equals(userId)) {
            throw new LimiteNaoEncontradoException(id);
        }
        return limite;
    }
}
