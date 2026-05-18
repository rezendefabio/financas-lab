package com.laboratorio.financas.incidente.domain;

import java.time.Instant;

public record FiltrosIncidente(
        Instant criadoApartirDe,
        Instant criadoAte,
        String classeErro,
        String operacao
) { }
