package com.laboratorio.financas.grupo.interfaces;

import com.laboratorio.financas.grupo.domain.Grupo;
import java.time.Instant;
import java.util.UUID;

public record GrupoResponse(
        UUID id,
        UUID userId,
        String nome,
        String descricao,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static GrupoResponse fromDomain(Grupo grupo) {
        return new GrupoResponse(
                grupo.getId(),
                grupo.getUserId(),
                grupo.getNome(),
                grupo.getDescricao(),
                grupo.isAtivo(),
                grupo.getCriadoEm(),
                grupo.getAtualizadoEm()
        );
    }
}
