package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import com.laboratorio.financas.transacao.domain.Transacao;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransacaoMapper {

    default TransacaoEntity toEntity(Transacao transacao) {
        if (transacao == null) {
            return null;
        }
        return new TransacaoEntity(
                transacao.getId(),
                transacao.getTipo(),
                toMoneyEmbeddable(transacao.getValor()),
                transacao.getData(),
                transacao.getDescricao(),
                transacao.getContaId(),
                transacao.getContaDestinoId(),
                transacao.getCategoriaId(),
                transacao.getCriadoEm(),
                transacao.getAtualizadoEm()
        );
    }

    default Transacao toDomain(TransacaoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Transacao(
                entity.getId(),
                entity.getTipo(),
                toMoney(entity.getValor()),
                entity.getData(),
                entity.getDescricao(),
                entity.getContaId(),
                entity.getContaDestinoId(),
                entity.getCategoriaId(),
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
