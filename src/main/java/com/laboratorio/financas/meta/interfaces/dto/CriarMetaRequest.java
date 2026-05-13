package com.laboratorio.financas.meta.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarMetaRequest(
        @NotBlank
        @Size(max = 200)
        String nome
) { }
