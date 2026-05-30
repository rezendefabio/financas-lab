package com.laboratorio.financas.emprestimo.domain;

import java.util.UUID;

public class EmprestimoNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public EmprestimoNaoEncontradoException(UUID id) {
        super("Emprestimo nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
