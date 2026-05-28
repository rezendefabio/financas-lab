package com.laboratorio.financas.fatura.application;

import com.laboratorio.financas.fatura.domain.FaturaNaoEncontradaException;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarFaturaUseCase {

    private final FaturaRepository repository;

    public DeletarFaturaUseCase(FaturaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new FaturaNaoEncontradaException(id));
        repository.deletar(id);
    }
}
