package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarCategoriaUseCase {

    private final CategoriaRepository repository;

    public DeletarCategoriaUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new CategoriaNaoEncontradaException(id);
        }
        repository.deletar(id);
    }
}
