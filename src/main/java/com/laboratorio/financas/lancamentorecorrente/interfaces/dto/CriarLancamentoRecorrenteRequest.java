package com.laboratorio.financas.lancamentorecorrente.interfaces.dto;

import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarLancamentoRecorrenteRequest(
        @NotBlank @Size(max = 200) String descricao,
        @NotNull TipoTransacao tipo,
        @NotNull BigDecimal valorValor,
        @NotNull @Size(min = 3, max = 3) String valorMoeda,
        @NotNull UUID contaId,
        UUID categoriaId,
        @NotNull Periodicidade periodicidade,
        @NotNull LocalDate proximaOcorrencia
) { }
