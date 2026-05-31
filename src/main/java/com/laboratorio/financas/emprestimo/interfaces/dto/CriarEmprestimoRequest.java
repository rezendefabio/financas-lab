package com.laboratorio.financas.emprestimo.interfaces.dto;

import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarEmprestimoRequest(
        @NotBlank @Size(max = 100) String descricao,
        @Size(max = 100) String nomeTerceiro,
        @NotNull TipoEmprestimo tipo,
        @NotNull @Positive BigDecimal valor,
        @NotNull @Size(min = 3, max = 3) String moeda,
        @NotNull LocalDate dataEmprestimo,
        boolean quitado) {
}
