package com.laboratorio.financas.categoria.domain;

import java.util.UUID;

public class CategoriaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public CategoriaNaoEncontradaException(UUID id) {
        super("Categoria nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
