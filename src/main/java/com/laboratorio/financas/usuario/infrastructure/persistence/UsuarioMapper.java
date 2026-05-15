package com.laboratorio.financas.usuario.infrastructure.persistence;

import com.laboratorio.financas.usuario.domain.Usuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    default UsuarioEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioEntity(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getSenhaHash(),
                usuario.isAtivo(),
                usuario.getCriadoEm(),
                usuario.getName(),
                usuario.getUpdatedAt()
        );
    }

    default Usuario toUsuario(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Usuario(
                entity.getId(),
                entity.getEmail(),
                entity.getSenhaHash(),
                entity.isAtivo(),
                entity.getCriadoEm(),
                entity.getName(),
                entity.getUpdatedAt()
        );
    }
}
