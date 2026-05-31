package com.laboratorio.financas.assinatura.interfaces.dto;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
import java.math.BigDecimal;
import java.util.UUID;

public record AssinaturaResponse(
        UUID id,
        UUID userId,
        String nome,
        TipoAssinatura tipo,
        ValorMonetario valorMensal,
        String dataRenovacao,
        boolean ativa,
        String criadoEm,
        String atualizadoEm
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static AssinaturaResponse fromDomain(Assinatura domain) {
        return new AssinaturaResponse(
                domain.getId(),
                domain.getUserId(),
                domain.getNome(),
                domain.getTipo(),
                new ValorMonetario(
                        domain.getValorMensal().valor(),
                        domain.getValorMensal().moeda().getCurrencyCode()),
                domain.getDataRenovacao().toString(),
                domain.isAtiva(),
                domain.getCriadoEm().toString(),
                domain.getAtualizadoEm().toString()
        );
    }
}
