package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarCategoriasUseCase {

    private final CategoriaRepository repository;

    public ListarCategoriasUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> executar(TipoCategoria tipo) {
        if (tipo == null) {
            return repository.listarTodas();
        }
        return repository.listarPorTipo(tipo);
    }
}
