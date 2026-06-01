package com.laboratorio.financas.limite.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimiteRepository {
    Limite salvar(Limite limite);

    Optional<Limite> buscarPorId(UUID id);

    List<Limite> listarTodos();

    Limite atualizar(Limite limite);

    void deletar(UUID id);
}
