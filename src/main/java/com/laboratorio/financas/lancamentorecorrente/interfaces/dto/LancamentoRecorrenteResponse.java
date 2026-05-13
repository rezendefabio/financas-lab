package com.laboratorio.financas.lancamentorecorrente.interfaces.dto;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoRecorrenteResponse(
        UUID id,
        String descricao,
        TipoTransacao tipo,
        ValorMonetario valor,
        UUID contaId,
        UUID categoriaId,
        Periodicidade periodicidade,
        LocalDate proximaOcorrencia,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static LancamentoRecorrenteResponse fromDomain(LancamentoRecorrente lancamento) {
        return new LancamentoRecorrenteResponse(
                lancamento.getId(),
                lancamento.getDescricao(),
                lancamento.getTipo(),
                new ValorMonetario(
                        lancamento.getValor().valor(),
                        lancamento.getValor().moeda().getCurrencyCode()
                ),
                lancamento.getContaId(),
                lancamento.getCategoriaId(),
                lancamento.getPeriodicidade(),
                lancamento.getProximaOcorrencia(),
                lancamento.isAtivo(),
                lancamento.getCriadoEm(),
                lancamento.getAtualizadoEm()
        );
    }
}
