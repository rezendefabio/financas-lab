package com.laboratorio.financas.assinatura.infrastructure.persistence;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssinaturaMapper {

    default AssinaturaEntity toEntity(Assinatura domain) {
        if (domain == null) {
            return null;
        }
        return new AssinaturaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getNome(),
                domain.getTipo(),
                new MoneyEmbeddable(
                        domain.getValorMensal().valor(),
                        domain.getValorMensal().moeda().getCurrencyCode()),
                domain.getDataRenovacao(),
                domain.isAtiva(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default Assinatura toDomain(AssinaturaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Assinatura(
                entity.getId(),
                entity.getUserId(),
                entity.getNome(),
                entity.getTipo(),
                new Money(
                        entity.getValorMensal().getValor(),
                        Currency.getInstance(entity.getValorMensal().getMoeda())),
                entity.getDataRenovacao(),
                entity.isAtiva(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
