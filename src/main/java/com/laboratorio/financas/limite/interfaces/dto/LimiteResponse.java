package com.laboratorio.financas.limite.interfaces.dto;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.TipoLimite;
import java.math.BigDecimal;

public record LimiteResponse(
        String id,
        String userId,
        String nome,
        TipoLimite tipo,
        BigDecimal valor,
        boolean ativo,
        String criadoEm,
        String atualizadoEm
) {
    public static LimiteResponse fromDomain(Limite domain) {
        return new LimiteResponse(
                domain.getId().toString(),
                domain.getUserId().toString(),
                domain.getNome(),
                domain.getTipo(),
                domain.getValor().valor(),
                domain.isAtivo(),
                domain.getCriadoEm().toString(),
                domain.getAtualizadoEm().toString()
        );
    }
}
