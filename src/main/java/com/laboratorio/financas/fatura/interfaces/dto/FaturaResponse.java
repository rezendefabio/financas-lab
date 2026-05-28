package com.laboratorio.financas.fatura.interfaces.dto;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.UUID;

public record FaturaResponse(
        UUID id,
        UUID contaId,
        String nome,
        String dataVencimento,
        String dataFechamento,
        ValorMonetario valorTotal,
        boolean paga,
        String criadoEm,
        String atualizadoEm
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static FaturaResponse fromDomain(Fatura fatura) {
        Money valor = fatura.getValorTotal();
        ValorMonetario valorTotal = valor != null
                ? new ValorMonetario(valor.valor(), valor.moeda().getCurrencyCode())
                : null;
        return new FaturaResponse(
                fatura.getId(),
                fatura.getContaId(),
                fatura.getNome(),
                fatura.getDataVencimento().toString(),
                fatura.getDataFechamento() != null ? fatura.getDataFechamento().toString() : null,
                valorTotal,
                fatura.isPaga(),
                fatura.getCriadoEm().toString(),
                fatura.getAtualizadoEm().toString()
        );
    }
}
