package com.laboratorio.financas.grupo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GrupoRepository {

    Grupo salvar(Grupo grupo);

    Optional<Grupo> buscarPorId(UUID id);

    Optional<Grupo> buscarPorIdEUserId(UUID id, UUID userId);

    List<Grupo> listarTodos();

    void deletar(UUID id);
}
