package com.laboratorio.financas.anotacao.domain;

import java.util.UUID;

public class AnotacaoNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public AnotacaoNaoEncontradaException(UUID id) {
        super("Anotacao nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
