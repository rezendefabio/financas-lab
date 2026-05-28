package com.laboratorio.financas.grupo.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrupoRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @Size(max = 300)
        String descricao
) { }
