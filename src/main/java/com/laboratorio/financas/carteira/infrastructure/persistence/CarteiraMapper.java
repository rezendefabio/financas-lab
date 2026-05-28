package com.laboratorio.financas.carteira.infrastructure.persistence;

import com.laboratorio.financas.carteira.domain.Carteira;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CarteiraMapper {

    default CarteiraEntity toEntity(Carteira carteira) {
        if (carteira == null) {
            return null;
        }
        return new CarteiraEntity(
                carteira.getId(),
                carteira.getUserId(),
                carteira.getContaId(),
                carteira.getNome(),
                carteira.getTipo(),
                carteira.isAtivo(),
                carteira.getCriadoEm(),
                carteira.getAtualizadoEm()
        );
    }

    default Carteira toDomain(CarteiraEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Carteira(
                entity.getId(),
                entity.getUserId(),
                entity.getContaId(),
                entity.getNome(),
                entity.getTipo(),
                entity.isAtivo(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
