package com.laboratorio.financas.centrocusto.infrastructure.persistence;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CentroCustoMapper {

    default CentroCustoEntity toEntity(CentroCusto domain) {
        if (domain == null) {
            return null;
        }
        return new CentroCustoEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getNome(),
                domain.getDescricao(),
                domain.isAtivo(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default CentroCusto toDomain(CentroCustoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CentroCusto(
                entity.getId(),
                entity.getUserId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.isAtivo(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
