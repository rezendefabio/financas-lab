package com.laboratorio.financas.tag.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank
        @Size(max = 50)
        String nome,

        @Size(max = 7)
        String cor
) { }
