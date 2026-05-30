package com.laboratorio.financas.lembrete.interfaces.rest;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.PrioridadeLembrete;
import java.time.LocalDate;

public record LembreteResponse(
        String id,
        String userId,
        String titulo,
        String descricao,
        LocalDate dataLembrete,
        PrioridadeLembrete prioridade,
        boolean concluido,
        String criadoEm,
        String atualizadoEm
) {
    public static LembreteResponse fromDomain(Lembrete domain) {
        return new LembreteResponse(
                domain.getId().toString(),
                domain.getUserId().toString(),
                domain.getTitulo(),
                domain.getDescricao(),
                domain.getDataLembrete(),
                domain.getPrioridade(),
                domain.isConcluido(),
                domain.getCriadoEm().toString(),
                domain.getAtualizadoEm() != null ? domain.getAtualizadoEm().toString() : null
        );
    }
}
