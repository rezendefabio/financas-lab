package com.laboratorio.financas.transacao.domain;

import java.time.LocalDate;
import java.util.UUID;

public record FiltrosTransacao(
        UUID contaId,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoTransacao tipo,
        UUID categoriaId,
        UUID userId
) {
    /** Construtor de compatibilidade retroativa sem userId -- userId fica null (sem filtro). */
    public FiltrosTransacao(UUID contaId, LocalDate dataInicio, LocalDate dataFim,
                            TipoTransacao tipo, UUID categoriaId) {
        this(contaId, dataInicio, dataFim, tipo, categoriaId, null);
    }
}
