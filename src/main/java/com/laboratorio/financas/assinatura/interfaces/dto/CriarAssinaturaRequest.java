package com.laboratorio.financas.assinatura.interfaces.dto;

import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarAssinaturaRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotNull TipoAssinatura tipo,
        @NotNull BigDecimal valorMensal,
        @NotNull @Size(min = 3, max = 3) String moeda,
        @NotNull LocalDate dataRenovacao
) { }
