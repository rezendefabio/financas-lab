package com.laboratorio.financas.anexo.infrastructure.persistence;

import com.laboratorio.financas.anexo.domain.Anexo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnexoMapper {

    default AnexoEntity toEntity(Anexo domain) {
        if (domain == null) {
            return null;
        }
        return new AnexoEntity(domain.getId(), domain.getNome(), domain.getCriadoEm());
    }

    default Anexo toDomain(AnexoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Anexo(entity.getId(), entity.getNome(), entity.getCriadoEm());
    }
}
