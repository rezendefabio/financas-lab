package com.laboratorio.financas.orcamento.infrastructure.persistence;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrcamentoMapper {

    default OrcamentoEntity toEntity(Orcamento domain) {
        if (domain == null) {
            return null;
        }
        return new OrcamentoEntity(domain.getId(), domain.getNome(), domain.getCriadoEm());
    }

    default Orcamento toDomain(OrcamentoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Orcamento(entity.getId(), entity.getNome(), entity.getCriadoEm());
    }
}
