package com.laboratorio.financas.relatorio.interfaces.dto;

import com.laboratorio.financas.relatorio.application.GastosPorCategoriaUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record GastosPorCategoriaResponse(
        LocalDate dataInicio,
        LocalDate dataFim,
        ValorMonetario totalGeral,
        List<ItemGastoResponse> itensPorCategoria
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public record ItemGastoResponse(
            UUID categoriaId,
            String nomeCategoria,
            ValorMonetario totalGasto
    ) { }

    public static GastosPorCategoriaResponse fromResultado(GastosPorCategoriaUseCase.Resultado r) {
        List<ItemGastoResponse> itens = r.itensPorCategoria().stream()
                .map(item -> new ItemGastoResponse(
                        item.categoriaId(),
                        item.nomeCategoria(),
                        new ValorMonetario(
                                item.totalGasto().valor(),
                                item.totalGasto().moeda().getCurrencyCode()
                        )
                ))
                .collect(Collectors.toList());

        return new GastosPorCategoriaResponse(
                r.dataInicio(),
                r.dataFim(),
                new ValorMonetario(
                        r.totalGeral().valor(),
                        r.totalGeral().moeda().getCurrencyCode()
                ),
                itens
        );
    }
}
