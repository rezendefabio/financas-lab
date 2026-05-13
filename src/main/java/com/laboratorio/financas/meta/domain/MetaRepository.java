package com.laboratorio.financas.meta.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaRepository {

    Meta salvar(Meta meta);

    Optional<Meta> buscarPorId(UUID id);

    List<Meta> listar();

    Meta atualizar(Meta meta);
}
