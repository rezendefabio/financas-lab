package com.laboratorio.financas.centrocusto.application.dto;

import java.util.UUID;

public record AtualizarCentroCustoComando(
        UUID id,
        String nome,
        String descricao
) { }
