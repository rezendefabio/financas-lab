package com.laboratorio.financas.centrocusto.application;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoNaoEncontradoException;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarCentroCustoPorIdUseCase {

    private final CentroCustoRepository repository;

    public BuscarCentroCustoPorIdUseCase(CentroCustoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CentroCusto executar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new CentroCustoNaoEncontradoException(id));
    }
}
