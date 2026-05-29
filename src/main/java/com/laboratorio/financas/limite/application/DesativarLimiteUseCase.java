package com.laboratorio.financas.limite.application;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteNaoEncontradoException;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DesativarLimiteUseCase {

    private final LimiteRepository repository;

    public DesativarLimiteUseCase(LimiteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id, UUID userId) {
        Limite limite = repository.buscarPorId(id)
                .orElseThrow(() -> new LimiteNaoEncontradoException(id));
        if (!limite.getUserId().equals(userId)) {
            throw new LimiteNaoEncontradoException(id);
        }
        limite.desativar();
        repository.atualizar(limite);
    }
}
