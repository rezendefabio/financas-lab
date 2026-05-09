package com.laboratorio.financas.conta.domain;

import java.util.UUID;

public class ContaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public ContaNaoEncontradaException(UUID id) {
        super("Conta nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
