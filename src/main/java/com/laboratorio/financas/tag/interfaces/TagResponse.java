package com.laboratorio.financas.tag.interfaces;

import com.laboratorio.financas.tag.domain.Tag;
import java.time.Instant;
import java.util.UUID;

public record TagResponse(
        UUID id,
        UUID userId,
        String nome,
        String cor,
        Instant criadoEm
) {

    public static TagResponse fromDomain(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getUserId(),
                tag.getNome(),
                tag.getCor(),
                tag.getCriadoEm()
        );
    }
}
