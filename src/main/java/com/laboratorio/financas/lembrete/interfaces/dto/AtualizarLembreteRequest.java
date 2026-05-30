package com.laboratorio.financas.lembrete.interfaces.dto;

import com.laboratorio.financas.lembrete.domain.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AtualizarLembreteRequest(
        @NotBlank @Size(max = 100) String titulo,
        @Size(max = 500) String descricao,
        @NotNull LocalDate dataLembrete,
        @NotNull Prioridade prioridade,
        @NotNull Boolean concluido
) {
}
