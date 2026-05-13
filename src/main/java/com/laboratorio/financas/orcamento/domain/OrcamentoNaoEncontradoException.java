package com.laboratorio.financas.orcamento.domain;

import java.util.UUID;

public class OrcamentoNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public OrcamentoNaoEncontradoException(UUID id) {
        super("Orcamento nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
