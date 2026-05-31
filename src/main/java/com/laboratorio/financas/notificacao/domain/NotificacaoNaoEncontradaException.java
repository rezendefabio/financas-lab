package com.laboratorio.financas.notificacao.domain;

import java.util.UUID;

public class NotificacaoNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public NotificacaoNaoEncontradaException(UUID id) {
        super("Notificacao nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
