package com.laboratorio.financas.centrocusto.infrastructure.persistence;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CentroCustoRepositoryImpl implements CentroCustoRepository {

    private final CentroCustoJpaRepository jpaRepository;
    private final CentroCustoMapper mapper;

    public CentroCustoRepositoryImpl(CentroCustoJpaRepository jpaRepository, CentroCustoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CentroCusto save(CentroCusto centroCusto) {
        CentroCustoEntity entity = mapper.toEntity(centroCusto);
        CentroCustoEntity salvo = jpaRepository.save(entity);
        return mapper.toDomain(salvo);
    }

    @Override
    public Optional<CentroCusto> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CentroCusto> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public List<CentroCusto> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
