package com.laboratorio.financas.lembrete.infrastructure.persistence;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LembreteMapper {

    default LembreteEntity toEntity(Lembrete domain) {
        if (domain == null) {
            return null;
        }
        return new LembreteEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getTitulo(),
                domain.getDescricao(),
                domain.getDataLembrete(),
                domain.getPrioridade(),
                domain.isConcluido(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default Lembrete toDomain(LembreteEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Lembrete(
                entity.getId(),
                entity.getUserId(),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.getDataLembrete(),
                entity.getPrioridade(),
                entity.isConcluido(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
