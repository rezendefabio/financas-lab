package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        String nome,
        TipoConta tipo,
        BigDecimal saldoInicialValor,
        String saldoInicialMoeda,
        boolean ativa,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static ContaResponse fromDomain(Conta conta) {
        return new ContaResponse(
                conta.getId(),
                conta.getNome(),
                conta.getTipo(),
                conta.getSaldoInicial().valor(),
                conta.getSaldoInicial().moeda().getCurrencyCode(),
                conta.isAtiva(),
                conta.getCriadoEm(),
                conta.getAtualizadoEm()
        );
    }
}
