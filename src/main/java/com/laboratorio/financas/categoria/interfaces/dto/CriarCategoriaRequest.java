package com.laboratorio.financas.categoria.interfaces.dto;

import com.laboratorio.financas.categoria.domain.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CriarCategoriaRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @NotNull
        TipoCategoria tipo,

        UUID categoriaPaiId,

        boolean system
) { }
