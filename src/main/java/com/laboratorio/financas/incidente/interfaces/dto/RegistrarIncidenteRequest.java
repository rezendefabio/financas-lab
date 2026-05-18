package com.laboratorio.financas.incidente.interfaces.dto;

public record RegistrarIncidenteRequest(
        String operacao,
        String classeErro,
        String mensagem,
        String stackTrace
) { }
