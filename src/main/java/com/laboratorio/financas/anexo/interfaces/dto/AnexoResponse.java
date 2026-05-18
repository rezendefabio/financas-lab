package com.laboratorio.financas.anexo.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record AnexoResponse(
        UUID id,
        String nome,
        Instant criadoEm
) { }
