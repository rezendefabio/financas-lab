package com.laboratorio.financas.instituicao.domain;

import java.util.UUID;

public class InstituicaoNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public InstituicaoNaoEncontradaException(UUID id) {
        super("Instituicao nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
