package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarCategoriaPorIdUseCase {

    private final CategoriaRepository repository;

    public BuscarCategoriaPorIdUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Categoria executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));
    }
}
