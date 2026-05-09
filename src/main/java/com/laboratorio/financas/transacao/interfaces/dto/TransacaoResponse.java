package com.laboratorio.financas.transacao.interfaces.dto;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        TipoTransacao tipo,
        BigDecimal valor,
        String moeda,
        LocalDate data,
        String descricao,
        UUID contaId,
        UUID contaDestinoId,
        UUID categoriaId,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static TransacaoResponse fromDomain(Transacao t) {
        return new TransacaoResponse(
                t.getId(),
                t.getTipo(),
                t.getValor().valor(),
                t.getValor().moeda().getCurrencyCode(),
                t.getData(),
                t.getDescricao(),
                t.getContaId(),
                t.getContaDestinoId(),
                t.getCategoriaId(),
                t.getCriadoEm(),
                t.getAtualizadoEm()
        );
    }
}
