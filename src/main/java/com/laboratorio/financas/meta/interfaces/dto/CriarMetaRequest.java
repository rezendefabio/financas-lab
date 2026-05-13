package com.laboratorio.financas.meta.interfaces.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarMetaRequest(
        @NotBlank String nome,
        @NotNull BigDecimal valorAlvoValor,
        @NotNull @Size(min = 3, max = 3) String valorAlvoMoeda,
        @NotNull @FutureOrPresent LocalDate prazo
) { }
