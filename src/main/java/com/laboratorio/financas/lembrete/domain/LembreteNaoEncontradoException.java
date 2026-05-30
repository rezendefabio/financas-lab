package com.laboratorio.financas.lembrete.domain;

import java.util.UUID;

public class LembreteNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public LembreteNaoEncontradoException(UUID id) {
        super("Lembrete nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
