package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.application.CalcularSaldoDaContaUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaldoResponse(
        UUID contaId,
        ValorMonetario saldoInicial,
        ValorMonetario totalReceitas,
        ValorMonetario totalDespesas,
        ValorMonetario totalTransferenciasEnviadas,
        ValorMonetario totalTransferenciasRecebidas,
        ValorMonetario saldoAtual,
        Instant calculadoEm
) {

    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static SaldoResponse fromResultado(CalcularSaldoDaContaUseCase.Resultado r) {
        return new SaldoResponse(
                r.contaId(),
                toValorMonetario(r.saldoInicial().valor(), r.saldoInicial().moeda().getCurrencyCode()),
                toValorMonetario(r.totalReceitas().valor(), r.totalReceitas().moeda().getCurrencyCode()),
                toValorMonetario(r.totalDespesas().valor(), r.totalDespesas().moeda().getCurrencyCode()),
                toValorMonetario(
                        r.totalTransferenciasEnviadas().valor(),
                        r.totalTransferenciasEnviadas().moeda().getCurrencyCode()
                ),
                toValorMonetario(
                        r.totalTransferenciasRecebidas().valor(),
                        r.totalTransferenciasRecebidas().moeda().getCurrencyCode()
                ),
                toValorMonetario(r.saldoAtual().valor(), r.saldoAtual().moeda().getCurrencyCode()),
                r.calculadoEm()
        );
    }

    private static ValorMonetario toValorMonetario(BigDecimal valor, String moeda) {
        return new ValorMonetario(valor, moeda);
    }
}
