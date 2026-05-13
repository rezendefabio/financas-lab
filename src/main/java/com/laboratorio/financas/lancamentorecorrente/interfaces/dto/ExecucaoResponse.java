package com.laboratorio.financas.lancamentorecorrente.interfaces.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ExecucaoResponse(
        UUID transacaoId,
        UUID lancamentoRecorrenteId,
        LocalDate dataExecutada,
        LocalDate novaProximaOcorrencia
) { }
