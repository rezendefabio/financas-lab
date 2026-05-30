package com.laboratorio.financas.emprestimo.interfaces.dto;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmprestimoResponse(
        UUID id,
        String descricao,
        String nomeTerceiro,
        TipoEmprestimo tipo,
        ValorMonetario valor,
        LocalDate dataEmprestimo,
        boolean quitado,
        String criadoEm,
        String atualizadoEm
) {

    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static EmprestimoResponse fromDomain(Emprestimo d) {
        return new EmprestimoResponse(
                d.getId(),
                d.getDescricao(),
                d.getNomeTerceiro(),
                d.getTipo(),
                new ValorMonetario(d.getValor().valor(), d.getValor().moeda().getCurrencyCode()),
                d.getDataEmprestimo(),
                d.isQuitado(),
                d.getCriadoEm().toString(),
                d.getAtualizadoEm().toString()
        );
    }
}
