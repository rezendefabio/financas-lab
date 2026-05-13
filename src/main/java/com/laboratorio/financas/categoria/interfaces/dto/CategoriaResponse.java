package com.laboratorio.financas.categoria.interfaces.dto;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.time.Instant;
import java.util.UUID;

public record CategoriaResponse(
        UUID id,
        String nome,
        TipoCategoria tipo,
        UUID categoriaPaiId,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static CategoriaResponse fromDomain(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getCategoriaPaiId(),
                categoria.getCriadoEm(),
                categoria.getAtualizadoEm()
        );
    }
}
