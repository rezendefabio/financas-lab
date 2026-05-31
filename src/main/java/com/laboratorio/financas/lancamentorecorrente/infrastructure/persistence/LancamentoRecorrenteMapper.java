package com.laboratorio.financas.lancamentorecorrente.infrastructure.persistence;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LancamentoRecorrenteMapper {

    default LancamentoRecorrenteEntity toEntity(LancamentoRecorrente lancamento) {
        if (lancamento == null) {
            return null;
        }
        return new LancamentoRecorrenteEntity(
                lancamento.getId(),
                lancamento.getUserId(),
                lancamento.getDescricao(),
                lancamento.getTipo(),
                toMoneyEmbeddable(lancamento.getValor()),
                lancamento.getContaId(),
                lancamento.getCategoriaId(),
                lancamento.getPeriodicidade(),
                lancamento.getProximaOcorrencia(),
                lancamento.isAtivo(),
                lancamento.getCriadoEm(),
                lancamento.getAtualizadoEm()
        );
    }

    default LancamentoRecorrente toDomain(LancamentoRecorrenteEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LancamentoRecorrente(
                entity.getId(),
                entity.getUserId(),
                entity.getDescricao(),
                entity.getTipo(),
                toMoney(entity.getValor()),
                entity.getContaId(),
                entity.getCategoriaId(),
                entity.getPeriodicidade(),
                entity.getProximaOcorrencia(),
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
