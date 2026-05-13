package com.laboratorio.financas.orcamento.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        String nome,
        Instant criadoEm
) { }
