package com.laboratorio.financas.notificacao.interfaces.dto;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import java.util.UUID;

public record NotificacaoResponse(
        UUID id,
        TipoNotificacao tipo,
        UUID referenciaId,
        String titulo,
        String descricao,
        String criadoEm
) {
    public static NotificacaoResponse fromDomain(Notificacao domain) {
        return new NotificacaoResponse(
                domain.getId(),
                domain.getTipo(),
                domain.getReferenciaId(),
                domain.getTitulo(),
                domain.getDescricao(),
                domain.getCriadoEm().toString()
        );
    }
}
