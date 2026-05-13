package com.laboratorio.financas.orcamento.interfaces.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        UUID categoriaId,
        ValorMonetario valorLimite,
        LocalDate mesAno,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }
}
