package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.domain.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CriarContaRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @NotNull
        TipoConta tipo,

        @NotNull
        BigDecimal saldoInicialValor,

        @NotNull
        @Size(min = 3, max = 3)
        String saldoInicialMoeda,

        UUID userId,

        BigDecimal limiteCreditoValor,

        @Size(min = 3, max = 3)
        String limiteCreditoMoeda,

        Integer diaFechamento,

        Integer diaVencimento
) { }
