package com.laboratorio.financas.payee.application.dto;

import java.util.UUID;

public record CriarPayeeComando(
        UUID userId,
        String nome,
        UUID categoriaPadraoId
) { }
