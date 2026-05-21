package com.laboratorio.financas.centrocusto.application.dto;

import java.util.UUID;

public record AtualizarCentroCustoComando(
        UUID id,
        UUID userId,
        String nome,
        String descricao
) { }
