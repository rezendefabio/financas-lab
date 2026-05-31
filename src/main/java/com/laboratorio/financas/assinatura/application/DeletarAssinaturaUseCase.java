package com.laboratorio.financas.assinatura.application;

import com.laboratorio.financas.assinatura.domain.AssinaturaNaoEncontradaException;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarAssinaturaUseCase {

    private final AssinaturaRepository repository;

    public DeletarAssinaturaUseCase(AssinaturaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new AssinaturaNaoEncontradaException(id));
        repository.deletar(id);
    }
}
