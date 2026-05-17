package com.laboratorio.financas.incidente.infrastructure.persistence;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ErroRegistradoMapper {

    default ErroRegistradoEntity toEntity(ErroRegistrado erro) {
        if (erro == null) {
            return null;
        }
        return new ErroRegistradoEntity(
                erro.getId(),
                erro.getCodigo(),
                erro.getOperacao(),
                erro.getClasseErro(),
                erro.getMensagem(),
                erro.getStackTrace(),
                erro.getCriadoEm()
        );
    }

    default ErroRegistrado toErroRegistrado(ErroRegistradoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ErroRegistrado(
                entity.getId(),
                entity.getCodigo(),
                entity.getOperacao(),
                entity.getClasseErro(),
                entity.getMensagem(),
                entity.getStackTrace(),
                entity.getCriadoEm()
        );
    }
}
