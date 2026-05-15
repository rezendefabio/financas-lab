package com.laboratorio.financas.instituicao.infrastructure.persistence;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstituicaoMapper {

    default InstituicaoEntity toEntity(Instituicao instituicao) {
        if (instituicao == null) {
            return null;
        }
        return new InstituicaoEntity(
                instituicao.getId(),
                instituicao.getNome(),
                instituicao.getCodigoBanco(),
                instituicao.getTipo(),
                instituicao.getLogoUrl(),
                instituicao.isAtiva(),
                instituicao.getCriadoEm()
        );
    }

    default Instituicao toDomain(InstituicaoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Instituicao(
                entity.getId(),
                entity.getNome(),
                entity.getCodigoBanco(),
                entity.getTipo(),
                entity.getLogoUrl(),
                entity.isAtiva(),
                entity.getCriadoEm()
        );
    }
}
