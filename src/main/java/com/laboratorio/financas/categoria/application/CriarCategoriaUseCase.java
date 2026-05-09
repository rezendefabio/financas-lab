package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarCategoriaUseCase {

    private final CategoriaRepository repository;

    public CriarCategoriaUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    public record Comando(String nome, TipoCategoria tipo) { }

    @Transactional
    public Categoria executar(Comando comando) {
        Categoria nova = new Categoria(comando.nome(), comando.tipo());
        return repository.salvar(nova);
    }
}
