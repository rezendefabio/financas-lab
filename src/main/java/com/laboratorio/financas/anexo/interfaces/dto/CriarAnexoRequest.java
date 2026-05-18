package com.laboratorio.financas.anexo.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarAnexoRequest(
        @NotBlank
        @Size(max = 100)
        String nome
) { }
