package com.laboratorio.financas.emprestimo.interfaces.dto;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import java.math.BigDecimal;
import java.util.UUID;

public record EmprestimoResponse(
        UUID id,
        String descricao,
        String nomeTerceiro,
        String tipo,
        ValorMonetario valor,
        String dataEmprestimo,
        boolean quitado,
        String criadoEm,
        String atualizadoEm) {

    public record ValorMonetario(BigDecimal valor, String moeda) {
    }

    public static EmprestimoResponse fromDomain(Emprestimo domain) {
        return new EmprestimoResponse(
                domain.getId(),
                domain.getDescricao(),
                domain.getNomeTerceiro(),
                domain.getTipo().name(),
                new ValorMonetario(domain.getValor().valor(),
                        domain.getValor().moeda().getCurrencyCode()),
                domain.getDataEmprestimo().toString(),
                domain.isQuitado(),
                domain.getCriadoEm().toString(),
                domain.getAtualizadoEm().toString());
    }
}
