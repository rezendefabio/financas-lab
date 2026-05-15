package com.laboratorio.financas.transacao.interfaces.dto;

import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransacaoRequest(
        @NotNull
        TipoTransacao tipo,

        @NotNull
        BigDecimal valor,

        @NotNull
        @Size(min = 3, max = 3)
        String moeda,

        @NotNull
        LocalDate data,

        @NotBlank
        @Size(max = 200)
        String descricao,

        @NotNull
        UUID contaId,

        /** Obrigatorio somente para TRANSFERENCIA (no backend vira par DESPESA+RECEITA). */
        UUID contaDestinoId,

        UUID categoriaId,

        /** Status da transacao. Padrao: CLEARED se nao informado. */
        StatusTransacao status,

        /** ID do payee (beneficiario). Sem FK constraint na Fase 1. */
        UUID payeeId,

        /** Tags associadas. Sem FK constraint na Fase 1. */
        List<UUID> tagIds
) { }
