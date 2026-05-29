package com.laboratorio.financas.limite.infrastructure.persistence;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LimiteMapper {

    default LimiteEntity toEntity(Limite domain) {
        if (domain == null) {
            return null;
        }
        return new LimiteEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getNome(),
                domain.getTipo(),
                toMoneyEmbeddable(domain.getValor()),
                domain.isAtivo(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default Limite toDomain(LimiteEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Limite(
                entity.getId(),
                entity.getUserId(),
                entity.getNome(),
                entity.getTipo(),
                toMoney(entity.getValor()),
                entity.isAtivo(),
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
        if (embeddable == null) {
            return null;
        }
        return new Money(embeddable.getValor(), Currency.getInstance(embeddable.getMoeda()));
    }
}
