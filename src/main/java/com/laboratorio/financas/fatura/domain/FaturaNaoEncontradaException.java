package com.laboratorio.financas.fatura.domain;

import java.util.UUID;

public class FaturaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public FaturaNaoEncontradaException(UUID id) {
        super("Fatura nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
