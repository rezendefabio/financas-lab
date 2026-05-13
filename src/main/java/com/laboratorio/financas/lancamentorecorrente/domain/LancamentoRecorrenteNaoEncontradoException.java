package com.laboratorio.financas.lancamentorecorrente.domain;

import java.util.UUID;

public class LancamentoRecorrenteNaoEncontradoException extends RuntimeException {

    private final UUID id;

    public LancamentoRecorrenteNaoEncontradoException(UUID id) {
        super("Lancamento recorrente nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
