package com.laboratorio.financas.meta.interfaces.dto;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.StatusMeta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MetaResponse(
        UUID id,
        String nome,
        ValorMonetario valorAlvo,
        ValorMonetario valorAtual,
        LocalDate prazo,
        String status,
        boolean atrasada,
        BigDecimal percentualConcluido,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static MetaResponse fromDomain(Meta meta) {
        BigDecimal percentual = meta.getValorAlvo().valor().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : meta.getValorAtual().valor()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(meta.getValorAlvo().valor(), 2, RoundingMode.HALF_UP);

        boolean atrasada = meta.getStatus() == StatusMeta.EM_ANDAMENTO
                && LocalDate.now().isAfter(meta.getPrazo());

        return new MetaResponse(
                meta.getId(),
                meta.getNome(),
                new ValorMonetario(meta.getValorAlvo().valor(), meta.getValorAlvo().moeda().getCurrencyCode()),
                new ValorMonetario(meta.getValorAtual().valor(), meta.getValorAtual().moeda().getCurrencyCode()),
                meta.getPrazo(),
                meta.getStatus().name(),
                atrasada,
                percentual,
                meta.getCriadoEm(),
                meta.getAtualizadoEm()
        );
    }
}
