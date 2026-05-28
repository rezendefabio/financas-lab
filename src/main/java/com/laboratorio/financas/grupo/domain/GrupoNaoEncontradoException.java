package com.laboratorio.financas.grupo.domain;

import java.util.UUID;

public class GrupoNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public GrupoNaoEncontradoException(UUID id) {
        super("Grupo nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
