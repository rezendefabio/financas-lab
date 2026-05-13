package com.laboratorio.financas.orcamento.infrastructure.persistence;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrcamentoMapper {

    default OrcamentoEntity toEntity(Orcamento orcamento) {
        if (orcamento == null) {
            return null;
        }
        return new OrcamentoEntity(
                orcamento.getId(),
                orcamento.getCategoriaId(),
                toMoneyEmbeddable(orcamento.getValorLimite()),
                orcamento.getMesAno(),
                orcamento.isAtivo(),
                orcamento.getCriadoEm(),
                orcamento.getAtualizadoEm()
        );
    }

    default Orcamento toOrcamento(OrcamentoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Orcamento(
                entity.getId(),
                entity.getCategoriaId(),
                toMoney(entity.getValorLimite()),
                entity.getMesAno(),
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
