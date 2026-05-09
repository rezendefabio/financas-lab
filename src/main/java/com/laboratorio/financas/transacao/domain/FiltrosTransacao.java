package com.laboratorio.financas.transacao.domain;

import java.time.LocalDate;
import java.util.UUID;

public record FiltrosTransacao(
        UUID contaId,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoTransacao tipo,
        UUID categoriaId
) { }
