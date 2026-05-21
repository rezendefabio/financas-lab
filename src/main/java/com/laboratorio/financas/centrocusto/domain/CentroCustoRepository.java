package com.laboratorio.financas.centrocusto.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CentroCustoRepository {

    CentroCusto save(CentroCusto centroCusto);

    Optional<CentroCusto> findById(UUID id);

    Optional<CentroCusto> findByIdAndUserId(UUID id, UUID userId);

    List<CentroCusto> findByUserId(UUID userId);
}
