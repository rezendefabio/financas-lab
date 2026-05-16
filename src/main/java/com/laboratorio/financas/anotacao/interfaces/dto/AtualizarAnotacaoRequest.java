package com.laboratorio.financas.anotacao.interfaces.dto;

import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AtualizarAnotacaoRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 5000) String conteudo,
        @NotNull TipoAnotacao tipo,
        @NotNull PrioridadeAnotacao prioridade,
        BigDecimal valorMontante,
        String valorMoeda,
        LocalDate dataReferencia
) { }
