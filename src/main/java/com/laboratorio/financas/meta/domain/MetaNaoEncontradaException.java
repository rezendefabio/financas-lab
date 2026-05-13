package com.laboratorio.financas.meta.domain;

import java.util.UUID;

public class MetaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public MetaNaoEncontradaException(UUID id) {
        super("Meta nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
