package com.laboratorio.financas.meta.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RegistrarDepositoRequest(
        @NotNull @Positive BigDecimal valor,
        @NotNull @Size(min = 3, max = 3) String moeda
) { }
