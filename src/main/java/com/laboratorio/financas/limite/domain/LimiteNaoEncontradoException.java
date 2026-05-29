package com.laboratorio.financas.limite.domain;

import java.util.UUID;

public class LimiteNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public LimiteNaoEncontradoException(UUID id) {
        super("Limite nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
