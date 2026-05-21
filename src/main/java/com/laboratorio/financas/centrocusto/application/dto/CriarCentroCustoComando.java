package com.laboratorio.financas.centrocusto.application.dto;

import java.util.UUID;

public record CriarCentroCustoComando(
        UUID userId,
        String nome,
        String descricao
) { }
