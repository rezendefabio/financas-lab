package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.FiltroGenerico;
import com.laboratorio.financas.transacao.domain.FiltroTransacaoCampo;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Constroi {@link Specification} de {@link TransacaoEntity} a partir de
 * {@link FiltrosTransacao}, incluindo os filtros adicionais campo-operador-valor.
 *
 * <p>Vive na camada de infraestrutura porque conhece o modelo de persistencia --
 * em particular que {@code valor} e um {@code @Embedded MoneyEmbeddable}
 * (path {@code valor.valor}) e que {@code data} de dominio mapeia para a
 * propriedade {@code data} da entidade. Usa Criteria API com predicados tipados,
 * sem concatenacao de strings, evitando SQL injection.
 *
 * <p>Os campos validos, seus tipos e os operadores aceitos sao regra de dominio
 * ({@link FiltroTransacaoCampo}); aqui apenas se traduz cada filtro num predicado.
 */
final class TransacaoSpecifications {

    private TransacaoSpecifications() {
    }

    /** Constroi a especificacao completa a partir dos filtros. */
    static Specification<TransacaoEntity> comFiltros(FiltrosTransacao filtros) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            predicados.add(cb.isNull(root.get("deletedAt")));

            if (filtros.contaId() != null) {
                predicados.add(cb.equal(root.get("contaId"), filtros.contaId()));
            }
            if (filtros.categoriaId() != null) {
                predicados.add(cb.equal(root.get("categoriaId"), filtros.categoriaId()));
            }
            if (filtros.userId() != null) {
                predicados.add(cb.equal(root.get("userId"), filtros.userId()));
            }
            if (filtros.tipo() != null) {
                predicados.add(cb.equal(root.get("tipo"), filtros.tipo()));
            }
            if (filtros.status() != null) {
                predicados.add(cb.equal(root.get("status"), filtros.status()));
            }
            if (filtros.dataInicio() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("data"), filtros.dataInicio()));
            }
            if (filtros.dataFim() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("data"), filtros.dataFim()));
            }

            for (FiltroGenerico fg : filtros.filtrosAdicionais()) {
                predicados.add(predicadoAdicional(root, cb, fg));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /** Traduz um {@link FiltroGenerico} num predicado tipado de Criteria API. */
    private static Predicate predicadoAdicional(
            Root<TransacaoEntity> root, CriteriaBuilder cb, FiltroGenerico fg) {
        FiltroTransacaoCampo campo = FiltroTransacaoCampo.fromNome(fg.campo());
        String operador = fg.operador();
        String valor = fg.valor();

        return switch (campo.tipo()) {
            case STRING -> predicadoString(cb, root.get(campo.nome()), operador, valor);
            // valor e um @Embedded MoneyEmbeddable: o numero vive em valor.valor.
            case NUMBER -> predicadoNumero(cb, root.get(campo.nome()).get("valor"), operador, valor);
            case DATE -> predicadoData(cb, root.get(campo.nome()), operador, valor);
        };
    }

    private static Predicate predicadoString(
            CriteriaBuilder cb, Path<String> path, String operador, String valor) {
        Expression<String> lowerCol = cb.lower(path);
        String lowerVal = valor.toLowerCase();
        return switch (operador) {
            case "contains" -> cb.like(lowerCol, "%" + escaparLike(lowerVal) + "%", '\\');
            case "not_contains" -> cb.notLike(lowerCol, "%" + escaparLike(lowerVal) + "%", '\\');
            case "eq" -> cb.equal(lowerCol, lowerVal);
            case "neq" -> cb.notEqual(lowerCol, lowerVal);
            default -> throw operadorInvalido(operador, "string");
        };
    }

    private static Predicate predicadoNumero(
            CriteriaBuilder cb, Path<BigDecimal> path, String operador, String valor) {
        BigDecimal num = new BigDecimal(valor.trim());
        return switch (operador) {
            case "eq" -> cb.equal(path, num);
            case "neq" -> cb.notEqual(path, num);
            case "gt" -> cb.greaterThan(path, num);
            case "gte" -> cb.greaterThanOrEqualTo(path, num);
            case "lt" -> cb.lessThan(path, num);
            case "lte" -> cb.lessThanOrEqualTo(path, num);
            default -> throw operadorInvalido(operador, "number");
        };
    }

    private static Predicate predicadoData(
            CriteriaBuilder cb, Path<LocalDate> path, String operador, String valor) {
        LocalDate data = LocalDate.parse(valor.trim());
        return switch (operador) {
            case "eq" -> cb.equal(path, data);
            case "neq" -> cb.notEqual(path, data);
            case "gt" -> cb.greaterThan(path, data);
            case "gte" -> cb.greaterThanOrEqualTo(path, data);
            case "lt" -> cb.lessThan(path, data);
            case "lte" -> cb.lessThanOrEqualTo(path, data);
            default -> throw operadorInvalido(operador, "date");
        };
    }

    /** Escapa os curingas LIKE ({@code %} e {@code _}) do valor informado pelo usuario. */
    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static IllegalArgumentException operadorInvalido(String operador, String tipo) {
        return new IllegalArgumentException(
                "Operador '" + operador + "' invalido para campo do tipo " + tipo);
    }
}
