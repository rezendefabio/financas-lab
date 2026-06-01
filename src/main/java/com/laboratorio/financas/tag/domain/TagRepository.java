package com.laboratorio.financas.tag.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository {

    Optional<Tag> buscarPorId(UUID id);

    List<Tag> listarTodos();

    List<Tag> buscarPorUserId(UUID userId);

    Optional<Tag> buscarPorIdEUserId(UUID id, UUID userId);

    Tag salvar(Tag tag);

    void deletar(UUID id);
}
