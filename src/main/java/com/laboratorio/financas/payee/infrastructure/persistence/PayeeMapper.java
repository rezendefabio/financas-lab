package com.laboratorio.financas.payee.infrastructure.persistence;

import com.laboratorio.financas.payee.domain.Payee;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayeeMapper {

    default PayeeEntity toEntity(Payee payee) {
        if (payee == null) {
            return null;
        }
        return new PayeeEntity(
                payee.getId(),
                payee.getUserId(),
                payee.getNome(),
                payee.getCategoriaPadraoId(),
                payee.getCriadoEm(),
                payee.getAtualizadoEm()
        );
    }

    default Payee toDomain(PayeeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Payee(
                entity.getId(),
                entity.getUserId(),
                entity.getNome(),
                entity.getCategoriaPadraoId(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
