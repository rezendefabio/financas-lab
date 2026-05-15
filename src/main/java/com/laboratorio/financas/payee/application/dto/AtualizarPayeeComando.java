package com.laboratorio.financas.payee.application.dto;

import java.util.UUID;

public record AtualizarPayeeComando(
        UUID id,
        UUID userId,
        String nome,
        UUID categoriaPadraoId
) { }
