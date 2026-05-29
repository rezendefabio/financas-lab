package com.laboratorio.financas.limite.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimiteRepository {
    Limite salvar(Limite limite);

    Optional<Limite> buscarPorId(UUID id);

    List<Limite> listarPorUserId(UUID userId);

    Limite atualizar(Limite limite);

    void deletar(UUID id);
}
