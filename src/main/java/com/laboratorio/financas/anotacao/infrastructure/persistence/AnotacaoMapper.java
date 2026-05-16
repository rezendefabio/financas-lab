package com.laboratorio.financas.anotacao.infrastructure.persistence;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnotacaoMapper {

    default AnotacaoEntity toEntity(Anotacao anotacao) {
        if (anotacao == null) {
            return null;
        }
        return new AnotacaoEntity(
                anotacao.getId(),
                anotacao.getUsuarioId(),
                anotacao.getTitulo(),
                anotacao.getConteudo(),
                anotacao.getTipo(),
                anotacao.getPrioridade(),
                toMoneyEmbeddable(anotacao.getValor()),
                anotacao.getDataReferencia(),
                anotacao.getCriadoEm(),
                anotacao.getAtualizadoEm()
        );
    }

    default Anotacao toAnotacao(AnotacaoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Anotacao(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getTitulo(),
                entity.getConteudo(),
                entity.getTipo(),
                entity.getPrioridade(),
                toMoney(entity.getValor()),
                entity.getDataReferencia(),
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
