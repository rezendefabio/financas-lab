package com.laboratorio.financas.centrocusto.domain;

import java.util.UUID;

public class CentroCustoNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public CentroCustoNaoEncontradoException(UUID id) {
        super("CentroCusto nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
