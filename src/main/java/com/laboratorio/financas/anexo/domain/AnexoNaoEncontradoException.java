package com.laboratorio.financas.anexo.domain;

import java.util.UUID;

public class AnexoNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public AnexoNaoEncontradoException(UUID id) {
        super("Anexo nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
