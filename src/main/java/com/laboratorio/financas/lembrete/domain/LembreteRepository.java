package com.laboratorio.financas.lembrete.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LembreteRepository {

    Lembrete salvar(Lembrete lembrete);

    Optional<Lembrete> buscarPorId(UUID id);

    List<Lembrete> listarTodos();

    Lembrete atualizar(Lembrete lembrete);

    void deletar(UUID id);
}
