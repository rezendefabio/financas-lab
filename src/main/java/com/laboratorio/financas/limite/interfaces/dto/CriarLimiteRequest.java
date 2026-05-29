package com.laboratorio.financas.limite.interfaces.dto;

import com.laboratorio.financas.limite.domain.TipoLimite;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CriarLimiteRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotNull TipoLimite tipo,
        @NotNull @Positive BigDecimal valor
) { }
