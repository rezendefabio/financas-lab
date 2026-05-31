package com.laboratorio.financas.assinatura.domain;

import java.util.UUID;

public class AssinaturaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public AssinaturaNaoEncontradaException(UUID id) {
        super("Assinatura nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
