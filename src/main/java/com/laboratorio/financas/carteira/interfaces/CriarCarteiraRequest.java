package com.laboratorio.financas.carteira.interfaces;

import com.laboratorio.financas.carteira.domain.TipoCarteira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CriarCarteiraRequest(
        @NotNull
        UUID contaId,

        @NotBlank
        @Size(max = 100)
        String nome,

        @NotNull
        TipoCarteira tipo
) { }
