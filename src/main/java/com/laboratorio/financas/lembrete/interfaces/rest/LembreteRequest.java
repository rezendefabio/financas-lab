package com.laboratorio.financas.lembrete.interfaces.rest;

import com.laboratorio.financas.lembrete.domain.PrioridadeLembrete;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LembreteRequest(
        @NotBlank @Size(max = 100) String titulo,
        @Size(max = 500) String descricao,
        @NotNull LocalDate dataLembrete,
        @NotNull PrioridadeLembrete prioridade,
        @NotNull Boolean concluido
) { }
