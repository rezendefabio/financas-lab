package com.laboratorio.financas.fatura.application;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarFaturasUseCase {

    private final FaturaRepository repository;

    public ListarFaturasUseCase(FaturaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Fatura> executar() {
        return repository.listarTodos();
    }
}
