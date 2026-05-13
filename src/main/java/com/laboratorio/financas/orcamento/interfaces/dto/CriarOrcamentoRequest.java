package com.laboratorio.financas.orcamento.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarOrcamentoRequest(
        @NotNull UUID categoriaId,
        @NotNull BigDecimal valorLimiteValor,
        @NotNull @Size(min = 3, max = 3) String valorLimiteMoeda,
        @NotNull LocalDate mesAno
) { }
