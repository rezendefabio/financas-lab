package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        UUID userId,
        String nome,
        TipoConta tipo,
        BigDecimal saldoInicialValor,
        String saldoInicialMoeda,
        BigDecimal saldoAtualValor,
        String saldoAtualMoeda,
        BigDecimal limiteCreditoValor,
        String limiteCreditoMoeda,
        Integer diaFechamento,
        Integer diaVencimento,
        boolean ativa,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static ContaResponse fromDomain(Conta conta) {
        Money limiteCredito = conta.getLimiteCredito();
        Money saldoAtual = conta.getSaldoAtual();
        return new ContaResponse(
                conta.getId(),
                conta.getUserId(),
                conta.getNome(),
                conta.getTipo(),
                conta.getSaldoInicial().valor(),
                conta.getSaldoInicial().moeda().getCurrencyCode(),
                saldoAtual != null ? saldoAtual.valor() : null,
                saldoAtual != null ? saldoAtual.moeda().getCurrencyCode() : null,
                limiteCredito != null ? limiteCredito.valor() : null,
                limiteCredito != null ? limiteCredito.moeda().getCurrencyCode() : null,
                conta.getDiaFechamento(),
                conta.getDiaVencimento(),
                conta.isAtiva(),
                conta.getCriadoEm(),
                conta.getAtualizadoEm()
        );
    }
}
