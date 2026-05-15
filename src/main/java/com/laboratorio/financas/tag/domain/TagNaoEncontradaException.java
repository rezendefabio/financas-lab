package com.laboratorio.financas.tag.domain;

import java.util.UUID;

public class TagNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public TagNaoEncontradaException(UUID id) {
        super("Tag nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
