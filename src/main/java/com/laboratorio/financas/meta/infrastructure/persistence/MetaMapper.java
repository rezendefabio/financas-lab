package com.laboratorio.financas.meta.infrastructure.persistence;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MetaMapper {

    default MetaEntity toEntity(Meta meta) {
        if (meta == null) {
            return null;
        }
        return new MetaEntity(
                meta.getId(),
                meta.getNome(),
                toMoneyEmbeddable(meta.getValorAlvo()),
                toMoneyEmbeddable(meta.getValorAtual()),
                meta.getPrazo(),
                meta.getStatus(),
                meta.getCriadoEm(),
                meta.getAtualizadoEm()
        );
    }

    default Meta toMeta(MetaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Meta(
                entity.getId(),
                entity.getNome(),
                toMoney(entity.getValorAlvo()),
                toMoney(entity.getValorAtual()),
                entity.getPrazo(),
                entity.getStatus(),
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
