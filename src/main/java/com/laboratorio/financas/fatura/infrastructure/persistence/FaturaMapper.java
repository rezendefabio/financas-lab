package com.laboratorio.financas.fatura.infrastructure.persistence;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FaturaMapper {

    default FaturaEntity toEntity(Fatura fatura) {
        if (fatura == null) {
            return null;
        }
        return new FaturaEntity(
                fatura.getId(),
                fatura.getUserId(),
                fatura.getContaId(),
                fatura.getNome(),
                fatura.getDataVencimento(),
                fatura.getDataFechamento(),
                toMoneyEmbeddable(fatura.getValorTotal()),
                fatura.isPaga(),
                fatura.getCriadoEm(),
                fatura.getAtualizadoEm()
        );
    }

    default Fatura toDomain(FaturaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Fatura(
                entity.getId(),
                entity.getUserId(),
                entity.getContaId(),
                entity.getNome(),
                entity.getDataVencimento(),
                entity.getDataFechamento(),
                toMoney(entity.getValorTotal()),
                entity.isPaga(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }

    default MoneyEmbeddable toMoneyEmbeddable(Money money) {
        if (money == null) {
            return null;
        }
        return new MoneyEmbeddable(money.valor(), money.moeda().getCurrencyCode());
    }

    default Money toMoney(MoneyEmbeddable embeddable) {
        if (embeddable == null || embeddable.getValor() == null || embeddable.getMoeda() == null) {
            return null;
        }
        return new Money(embeddable.getValor(), Currency.getInstance(embeddable.getMoeda()));
    }
}
