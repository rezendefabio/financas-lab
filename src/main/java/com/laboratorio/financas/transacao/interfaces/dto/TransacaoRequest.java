package com.laboratorio.financas.transacao.interfaces.dto;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoRequest(
        @NotNull
        TipoTransacao tipo,

        @NotNull
        BigDecimal valor,

        @NotNull
        @Size(min = 3, max = 3)
        String moeda,

        @NotNull
        LocalDate data,

        @NotBlank
        @Size(max = 200)
        String descricao,

        @NotNull
        UUID contaId,

        UUID contaDestinoId,

        UUID categoriaId
) { }
