package com.laboratorio.financas.emprestimo.infrastructure.persistence;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmprestimoMapper {

    default EmprestimoEntity toEntity(Emprestimo domain) {
        if (domain == null) {
            return null;
        }
        MoneyEmbeddable valor = new MoneyEmbeddable(
                domain.getValor().valor(),
                domain.getValor().moeda().getCurrencyCode());
        return new EmprestimoEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getDescricao(),
                domain.getNomeTerceiro(),
                domain.getTipo(),
                valor,
                domain.getDataEmprestimo(),
                domain.isQuitado(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default Emprestimo toDomain(EmprestimoEntity entity) {
        if (entity == null) {
            return null;
        }
        Money valor = new Money(
                entity.getValor().getValor(),
                Currency.getInstance(entity.getValor().getMoeda()));
        return new Emprestimo(
                entity.getId(),
                entity.getUserId(),
                entity.getDescricao(),
                entity.getNomeTerceiro(),
                entity.getTipo(),
                valor,
                entity.getDataEmprestimo(),
                entity.isQuitado(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
