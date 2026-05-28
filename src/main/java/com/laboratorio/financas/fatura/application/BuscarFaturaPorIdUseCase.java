package com.laboratorio.financas.fatura.application;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaNaoEncontradaException;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarFaturaPorIdUseCase {

    private final FaturaRepository repository;

    public BuscarFaturaPorIdUseCase(FaturaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Fatura executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new FaturaNaoEncontradaException(id));
    }
}
