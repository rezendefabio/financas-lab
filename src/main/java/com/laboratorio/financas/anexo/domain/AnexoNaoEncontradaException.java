package com.laboratorio.financas.anexo.domain;

import java.util.UUID;

public class AnexoNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public AnexoNaoEncontradaException(UUID id) {
        super("anexo nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
