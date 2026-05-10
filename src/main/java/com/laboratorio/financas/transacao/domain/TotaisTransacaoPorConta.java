package com.laboratorio.financas.transacao.domain;

import java.math.BigDecimal;

/**
 * Resultado da agregacao de transacoes por conta.
 *
 * Cada componente representa a soma absoluta de transacoes de um tipo
 * em que a conta participa. Calculo do saldo final fica a cargo do
 * caller (CalcularSaldoDaContaUseCase em conta/application/).
 *
 * Valores nunca sao null — agregacao vazia retorna zero (COALESCE no SQL).
 */
public record TotaisTransacaoPorConta(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal totalTransferenciasEnviadas,
        BigDecimal totalTransferenciasRecebidas
) { }
