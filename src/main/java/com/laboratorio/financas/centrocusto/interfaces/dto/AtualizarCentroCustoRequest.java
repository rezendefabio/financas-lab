package com.laboratorio.financas.centrocusto.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarCentroCustoRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @Size(max = 255)
        String descricao
) { }
