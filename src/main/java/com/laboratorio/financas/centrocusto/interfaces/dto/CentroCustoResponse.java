package com.laboratorio.financas.centrocusto.interfaces.dto;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import java.time.Instant;
import java.util.UUID;

public record CentroCustoResponse(
        UUID id,
        UUID userId,
        String nome,
        String descricao,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static CentroCustoResponse fromDomain(CentroCusto centroCusto) {
        return new CentroCustoResponse(
                centroCusto.getId(),
                centroCusto.getUserId(),
                centroCusto.getNome(),
                centroCusto.getDescricao(),
                centroCusto.isAtivo(),
                centroCusto.getCriadoEm(),
                centroCusto.getAtualizadoEm()
        );
    }
}
