package com.laboratorio.financas.transacao.domain;

import java.util.UUID;

public class TransacaoComReferenciaInvalidaException extends RuntimeException {

    private final String recurso;
    private final UUID id;

    public TransacaoComReferenciaInvalidaException(String recurso, UUID id) {
        super("Referencia invalida: " + recurso + " com id " + id + " nao existe");
        this.recurso = recurso;
        this.id = id;
    }

    public String getRecurso() {
        return recurso;
    }

    public UUID getId() {
        return id;
    }
}
