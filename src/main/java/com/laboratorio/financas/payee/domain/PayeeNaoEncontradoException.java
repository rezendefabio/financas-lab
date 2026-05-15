package com.laboratorio.financas.payee.domain;

import java.util.UUID;

public class PayeeNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public PayeeNaoEncontradoException(UUID id) {
        super("Payee nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
