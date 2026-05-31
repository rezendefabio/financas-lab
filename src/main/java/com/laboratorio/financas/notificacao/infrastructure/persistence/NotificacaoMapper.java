package com.laboratorio.financas.notificacao.infrastructure.persistence;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacaoMapper {

    default NotificacaoEntity toEntity(Notificacao domain) {
        if (domain == null) {
            return null;
        }
        return new NotificacaoEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getTipo(),
                domain.getReferenciaId(),
                domain.getTitulo(),
                domain.getDescricao(),
                domain.isDescartada(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default Notificacao toDomain(NotificacaoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Notificacao(
                entity.getId(),
                entity.getUserId(),
                entity.getTipo(),
                entity.getReferenciaId(),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.isDescartada(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
