package com.laboratorio.financas.meta.infrastructure.persistence;

import com.laboratorio.financas.meta.domain.Meta;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MetaMapper {

    default MetaEntity toEntity(Meta domain) {
        if (domain == null) {
            return null;
        }
        return new MetaEntity(domain.getId(), domain.getNome(), domain.getCriadoEm());
    }

    default Meta toMeta(MetaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Meta(entity.getId(), entity.getNome(), entity.getCriadoEm());
    }
}
