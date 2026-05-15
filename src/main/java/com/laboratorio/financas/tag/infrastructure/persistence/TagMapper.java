package com.laboratorio.financas.tag.infrastructure.persistence;

import com.laboratorio.financas.tag.domain.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {

    default TagEntity toEntity(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new TagEntity(
                tag.getId(),
                tag.getUserId(),
                tag.getNome(),
                tag.getCor(),
                tag.getCriadoEm()
        );
    }

    default Tag toDomain(TagEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Tag(
                entity.getId(),
                entity.getUserId(),
                entity.getNome(),
                entity.getCor(),
                entity.getCriadoEm()
        );
    }
}
