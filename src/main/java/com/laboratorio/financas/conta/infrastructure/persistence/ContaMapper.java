package com.laboratorio.financas.conta.infrastructure.persistence;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContaMapper {

    default ContaEntity toEntity(Conta conta) {
        if (conta == null) {
            return null;
        }
        return new ContaEntity(
                conta.getId(),
                conta.getUserId(),
                conta.getNome(),
                conta.getTipo(),
                toMoneyEmbeddable(conta.getSaldoInicial()),
                toMoneyEmbeddable(conta.getSaldoAtual()),
                toMoneyEmbeddable(conta.getLimiteCredito()),
                conta.getDiaFechamento(),
                conta.getDiaVencimento(),
                conta.isAtiva(),
                conta.getCriadoEm(),
                conta.getAtualizadoEm()
        );
    }

    default Conta toDomain(ContaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Conta(
                entity.getId(),
                entity.getUserId(),
                entity.getNome(),
                entity.getTipo(),
                toMoney(entity.getSaldoInicial()),
                toMoney(entity.getSaldoAtual()),
                toMoney(entity.getLimiteCredito()),
                entity.getDiaFechamento(),
                entity.getDiaVencimento(),
                entity.isAtiva(),
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
