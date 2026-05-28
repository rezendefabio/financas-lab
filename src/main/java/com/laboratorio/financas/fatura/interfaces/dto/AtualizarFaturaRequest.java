package com.laboratorio.financas.fatura.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AtualizarFaturaRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @NotNull
        LocalDate dataVencimento,

        LocalDate dataFechamento,

        BigDecimal valorTotalValor,

        @Size(min = 3, max = 3)
        String valorTotalMoeda
) { }
