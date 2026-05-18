package com.laboratorio.financas.incidente.domain;

public class IncidenteNaoEncontradoException extends RuntimeException {

    private final String codigo;

    public IncidenteNaoEncontradoException(String codigo) {
        super("Incidente nao encontrado: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
