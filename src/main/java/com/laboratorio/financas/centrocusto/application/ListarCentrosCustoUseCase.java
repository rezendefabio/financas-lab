package com.laboratorio.financas.centrocusto.application;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarCentrosCustoUseCase {

    private final CentroCustoRepository repository;

    public ListarCentrosCustoUseCase(CentroCustoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CentroCusto> executar() {
        return repository.listarTodos();
    }
}
