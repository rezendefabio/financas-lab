package com.laboratorio.financas.assinatura.application;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaNaoEncontradaException;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarAssinaturaPorIdUseCase {

    private final AssinaturaRepository repository;

    public BuscarAssinaturaPorIdUseCase(AssinaturaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Assinatura executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new AssinaturaNaoEncontradaException(id));
    }
}
