package com.laboratorio.financas.transacao.domain;

import java.time.LocalDate;
import java.util.UUID;

public record FiltrosTransacao(
        UUID contaId,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoTransacao tipo,
        UUID categoriaId,
        UUID userId,
        StatusTransacao status
) {
    /** Construtor de compatibilidade retroativa sem userId e sem status -- ambos ficam null (sem filtro). */
    public FiltrosTransacao(UUID contaId, LocalDate dataInicio, LocalDate dataFim,
                            TipoTransacao tipo, UUID categoriaId) {
        this(contaId, dataInicio, dataFim, tipo, categoriaId, null, null);
    }

    /** Construtor de compatibilidade retroativa sem status -- status fica null (sem filtro). */
    public FiltrosTransacao(UUID contaId, LocalDate dataInicio, LocalDate dataFim,
                            TipoTransacao tipo, UUID categoriaId, UUID userId) {
        this(contaId, dataInicio, dataFim, tipo, categoriaId, userId, null);
    }
}
