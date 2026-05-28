package com.laboratorio.financas.carteira.domain;

import java.util.UUID;

public class CarteiraNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public CarteiraNaoEncontradaException(UUID id) {
        super("Carteira nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
