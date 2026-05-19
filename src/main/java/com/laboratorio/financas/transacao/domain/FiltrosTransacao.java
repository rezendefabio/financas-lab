package com.laboratorio.financas.transacao.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FiltrosTransacao(
        UUID contaId,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoTransacao tipo,
        UUID categoriaId,
        UUID userId,
        StatusTransacao status,
        List<FiltroGenerico> filtrosAdicionais
) {
    /** Normaliza {@code filtrosAdicionais} nulo para lista vazia (sem filtros adicionais). */
    public FiltrosTransacao {
        filtrosAdicionais = (filtrosAdicionais != null) ? List.copyOf(filtrosAdicionais) : List.of();
    }

    /** Construtor de compatibilidade retroativa sem userId e sem status -- ambos ficam null (sem filtro). */
    public FiltrosTransacao(UUID contaId, LocalDate dataInicio, LocalDate dataFim,
                            TipoTransacao tipo, UUID categoriaId) {
        this(contaId, dataInicio, dataFim, tipo, categoriaId, null, null, List.of());
    }

    /** Construtor de compatibilidade retroativa sem status -- status fica null (sem filtro). */
    public FiltrosTransacao(UUID contaId, LocalDate dataInicio, LocalDate dataFim,
                            TipoTransacao tipo, UUID categoriaId, UUID userId) {
        this(contaId, dataInicio, dataFim, tipo, categoriaId, userId, null, List.of());
    }

    /** Construtor de compatibilidade retroativa sem filtros adicionais -- lista fica vazia. */
    public FiltrosTransacao(UUID contaId, LocalDate dataInicio, LocalDate dataFim,
                            TipoTransacao tipo, UUID categoriaId, UUID userId, StatusTransacao status) {
        this(contaId, dataInicio, dataFim, tipo, categoriaId, userId, status, List.of());
    }
}
