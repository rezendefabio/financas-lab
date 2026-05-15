package com.laboratorio.financas.transacao.interfaces.dto;

import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        TipoTransacao tipo,
        BigDecimal valor,
        String moeda,
        LocalDate data,
        String descricao,
        UUID contaId,
        UUID categoriaId,
        Instant criadoEm,
        Instant atualizadoEm,
        StatusTransacao status,
        UUID payeeId,
        List<UUID> tagIds,
        UUID transferGroupId
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
                t.getCategoriaId(),
                t.getCriadoEm(),
                t.getAtualizadoEm(),
                t.getStatus(),
                t.getPayeeId(),
                t.getTagIds(),
                t.getTransferGroupId()
        );
    }
}
