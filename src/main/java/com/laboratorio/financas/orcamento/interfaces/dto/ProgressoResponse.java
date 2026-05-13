package com.laboratorio.financas.orcamento.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProgressoResponse(
        UUID orcamentoId,
        UUID categoriaId,
        LocalDate mesAno,
        OrcamentoResponse.ValorMonetario valorLimite,
        OrcamentoResponse.ValorMonetario totalGasto,
        BigDecimal percentualUtilizado,
        String status
) { }
