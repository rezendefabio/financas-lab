package com.laboratorio.financas.relatorio.interfaces.dto;

import com.laboratorio.financas.relatorio.application.EvolucaoSaldoUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record EvolucaoSaldoResponse(
        LocalDate dataInicio,
        LocalDate dataFim,
        ValorMonetario totalReceitas,
        ValorMonetario totalDespesas,
        ValorMonetario saldoLiquido,
        List<ItemEvolucaoMesResponse> evolucaoPorMes
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public record ItemEvolucaoMesResponse(
            LocalDate mes,
            ValorMonetario totalReceitas,
            ValorMonetario totalDespesas,
            ValorMonetario saldoLiquido
    ) { }

    public static EvolucaoSaldoResponse fromResultado(EvolucaoSaldoUseCase.Resultado r) {
        List<ItemEvolucaoMesResponse> meses = r.evolucaoPorMes().stream()
                .map(item -> new ItemEvolucaoMesResponse(
                        item.mes(),
                        new ValorMonetario(
                                item.totalReceitas().valor(),
                                item.totalReceitas().moeda().getCurrencyCode()
                        ),
                        new ValorMonetario(
                                item.totalDespesas().valor(),
                                item.totalDespesas().moeda().getCurrencyCode()
                        ),
                        new ValorMonetario(
                                item.saldoLiquido().valor(),
                                item.saldoLiquido().moeda().getCurrencyCode()
                        )
                ))
                .collect(Collectors.toList());

        return new EvolucaoSaldoResponse(
                r.dataInicio(),
                r.dataFim(),
                new ValorMonetario(r.totalReceitas().valor(), r.totalReceitas().moeda().getCurrencyCode()),
                new ValorMonetario(r.totalDespesas().valor(), r.totalDespesas().moeda().getCurrencyCode()),
                new ValorMonetario(r.saldoLiquido().valor(), r.saldoLiquido().moeda().getCurrencyCode()),
                meses
        );
    }
}
