package com.laboratorio.financas.limite.application;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ListarLimitesUseCase {

    private final LimiteRepository repository;

    public ListarLimitesUseCase(LimiteRepository repository) {
        this.repository = repository;
    }

    public List<Limite> executar() {
        return repository.listarTodos();
    }
}
