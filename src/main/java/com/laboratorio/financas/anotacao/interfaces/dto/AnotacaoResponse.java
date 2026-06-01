package com.laboratorio.financas.anotacao.interfaces.dto;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AnotacaoResponse(
        UUID id,
        UUID userId,
        String titulo,
        String conteudo,
        String tipo,
        String prioridade,
        BigDecimal valorMontante,
        String valorMoeda,
        LocalDate dataReferencia,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static AnotacaoResponse fromDomain(Anotacao anotacao) {
        return new AnotacaoResponse(
                anotacao.getId(),
                anotacao.getUserId(),
                anotacao.getTitulo(),
                anotacao.getConteudo(),
                anotacao.getTipo().name(),
                anotacao.getPrioridade().name(),
                anotacao.getValor() != null ? anotacao.getValor().valor() : null,
                anotacao.getValor() != null ? anotacao.getValor().moeda().getCurrencyCode() : null,
                anotacao.getDataReferencia(),
                anotacao.getCriadoEm(),
                anotacao.getAtualizadoEm()
        );
    }
}
