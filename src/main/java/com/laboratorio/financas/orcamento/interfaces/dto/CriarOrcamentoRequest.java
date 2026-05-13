package com.laboratorio.financas.orcamento.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarOrcamentoRequest(
        @NotBlank
        @Size(max = 100)
        String nome
) { }
