package com.laboratorio.financas.importacao.interfaces.dto;

import java.math.BigDecimal;

public record AnaliseItemResponse(
        int linha,
        String linhaCsvOriginal,
        String tipo,
        BigDecimal valor,
        String moeda,
        String data,
        String descricao,
        String contaId,
        boolean possivelDuplicata,
        String transacaoExistenteId
) { }
