package com.laboratorio.financas.grupo.infrastructure.persistence;

import com.laboratorio.financas.grupo.domain.Grupo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GrupoMapper {

    default GrupoEntity toEntity(Grupo grupo) {
        if (grupo == null) {
            return null;
        }
        return new GrupoEntity(
                grupo.getId(),
                grupo.getUserId(),
                grupo.getNome(),
                grupo.getDescricao(),
                grupo.isAtivo(),
                grupo.getCriadoEm(),
                grupo.getAtualizadoEm()
        );
    }

    default Grupo toDomain(GrupoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Grupo(
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
