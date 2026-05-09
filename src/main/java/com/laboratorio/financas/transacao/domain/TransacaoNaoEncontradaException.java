package com.laboratorio.financas.transacao.domain;

import java.util.UUID;

public class TransacaoNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public TransacaoNaoEncontradaException(UUID id) {
        super("Transacao nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
