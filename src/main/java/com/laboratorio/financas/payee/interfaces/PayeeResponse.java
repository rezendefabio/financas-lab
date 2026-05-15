package com.laboratorio.financas.payee.interfaces;

import com.laboratorio.financas.payee.domain.Payee;
import java.time.Instant;
import java.util.UUID;

public record PayeeResponse(
        UUID id,
        UUID userId,
        String nome,
        UUID categoriaPadraoId,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static PayeeResponse fromDomain(Payee payee) {
        return new PayeeResponse(
                payee.getId(),
                payee.getUserId(),
                payee.getNome(),
                payee.getCategoriaPadraoId(),
                payee.getCriadoEm(),
                payee.getAtualizadoEm()
        );
    }
}
