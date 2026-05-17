package com.laboratorio.financas.incidente.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record IncidenteResponse(
        UUID id,
        String codigo,
        String operacao,
        String classeErro,
        String mensagem,
        String stackTrace,
        Instant criadoEm
) { }
