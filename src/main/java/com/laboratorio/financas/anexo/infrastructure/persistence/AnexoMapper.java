package com.laboratorio.financas.anexo.infrastructure.persistence;

import com.laboratorio.financas.anexo.domain.Anexo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnexoMapper {

    default AnexoEntity toEntity(Anexo anexo) {
        if (anexo == null) {
            return null;
        }
        return new AnexoEntity(
                anexo.getId(),
                anexo.getNome(),
                anexo.getTipoConteudo(),
                anexo.getTamanho(),
                anexo.getChaveArmazenamento(),
                anexo.getEntidadeTipo(),
                anexo.getEntidadeId(),
                anexo.getCriadoEm()
        );
    }

    default Anexo toDomain(AnexoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Anexo(
                entity.getId(),
                entity.getNome(),
                entity.getTipoConteudo(),
                entity.getTamanho(),
                entity.getChaveArmazenamento(),
                entity.getEntidadeTipo(),
                entity.getEntidadeId(),
                entity.getCriadoEm()
        );
    }
}
