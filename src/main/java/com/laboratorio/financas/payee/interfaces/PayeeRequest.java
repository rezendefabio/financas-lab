package com.laboratorio.financas.payee.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PayeeRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        UUID categoriaPadraoId
) { }
