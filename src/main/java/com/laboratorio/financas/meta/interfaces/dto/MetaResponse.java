package com.laboratorio.financas.meta.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record MetaResponse(
        UUID id,
        String nome,
        Instant criadoEm
) { }
