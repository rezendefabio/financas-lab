package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import com.laboratorio.financas.transacao.domain.Transacao;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
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
                transacao.getCategoriaId(),
                transacao.getCriadoEm(),
                transacao.getAtualizadoEm(),
                transacao.getUserId(),
                transacao.getStatus(),
                transacao.getDeletedAt(),
                transacao.getPayeeId(),
                transacao.getTransferGroupId(),
                transacao.getTransferPairId(),
                new HashSet<>(transacao.getTagIds())
        );
    }

    default Transacao toDomain(TransacaoEntity entity) {
        if (entity == null) {
            return null;
        }
        List<UUID> tagIds = new ArrayList<>(entity.getTagIds());
        return new Transacao(
                entity.getId(),
                entity.getTipo(),
                toMoney(entity.getValor()),
                entity.getData(),
                entity.getDescricao(),
                entity.getContaId(),
                entity.getCategoriaId(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getDeletedAt(),
                entity.getPayeeId(),
                entity.getTransferGroupId(),
                entity.getTransferPairId(),
                tagIds
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
