package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.Categoria;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    default CategoriaEntity toEntity(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaEntity(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getCriadoEm(),
                categoria.getAtualizadoEm()
        );
    }

    default Categoria toDomain(CategoriaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Categoria(
                entity.getId(),
                entity.getNome(),
                entity.getTipo(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
