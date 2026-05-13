package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.application.CalcularSaldoTotalUseCase;
import java.math.BigDecimal;

public record SaldoTotalResponse(BigDecimal valor, String moeda, int totalContas) {

    public static SaldoTotalResponse fromResultado(CalcularSaldoTotalUseCase.Resultado r) {
        return new SaldoTotalResponse(
                r.saldoTotal().valor(),
                r.saldoTotal().moeda().getCurrencyCode(),
                r.totalContas()
        );
    }
}
