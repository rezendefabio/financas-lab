package com.laboratorio.financas.categoria.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository {

    Categoria salvar(Categoria categoria);

    Optional<Categoria> buscarPorId(UUID id);

    List<Categoria> listarTodas();

    List<Categoria> listarPorTipo(TipoCategoria tipo);

    void deletar(UUID id);
}
