package com.laboratorio.financas.carteira.interfaces;

import com.laboratorio.financas.carteira.domain.Carteira;
import java.util.UUID;

public record CarteiraResponse(
        UUID id,
        UUID contaId,
        String nome,
        String tipo,
        boolean ativo,
        String criadoEm,
        String atualizadoEm
) {

    public static CarteiraResponse fromDomain(Carteira carteira) {
        return new CarteiraResponse(
                carteira.getId(),
                carteira.getContaId(),
                carteira.getNome(),
                carteira.getTipo().name(),
                carteira.isAtivo(),
                carteira.getCriadoEm().toString(),
                carteira.getAtualizadoEm().toString()
        );
    }
}
