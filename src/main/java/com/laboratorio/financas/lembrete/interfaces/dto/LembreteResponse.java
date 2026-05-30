package com.laboratorio.financas.lembrete.interfaces.dto;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.Prioridade;
import java.time.LocalDate;
import java.util.UUID;

public record LembreteResponse(
        UUID id,
        String titulo,
        String descricao,
        LocalDate dataLembrete,
        Prioridade prioridade,
        boolean concluido,
        String criadoEm,
        String atualizadoEm
) {
    public static LembreteResponse fromDomain(Lembrete domain) {
        return new LembreteResponse(
                domain.getId(),
                domain.getTitulo(),
                domain.getDescricao(),
                domain.getDataLembrete(),
                domain.getPrioridade(),
                domain.isConcluido(),
                domain.getCriadoEm().toString(),
                domain.getAtualizadoEm().toString()
        );
    }
}
