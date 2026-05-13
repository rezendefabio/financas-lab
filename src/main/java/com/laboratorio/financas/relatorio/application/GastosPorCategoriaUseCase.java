package com.laboratorio.financas.relatorio.application;

import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GastosPorCategoriaUseCase {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;

    public GastosPorCategoriaUseCase(TransacaoRepository transacaoRepository,
                                     CategoriaRepository categoriaRepository) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public record Consulta(LocalDate dataInicio, LocalDate dataFim, UUID contaId) { }

    public record ItemGastoPorCategoria(UUID categoriaId, String nomeCategoria, Money totalGasto) { }

    public record Resultado(
            LocalDate dataInicio,
            LocalDate dataFim,
            Money totalGeral,
            List<ItemGastoPorCategoria> itensPorCategoria
    ) { }

    @Transactional(readOnly = true)
    public Resultado executar(Consulta consulta) {
        FiltrosTransacao filtros = new FiltrosTransacao(
                consulta.contaId(),
                consulta.dataInicio(),
                consulta.dataFim(),
                TipoTransacao.DESPESA,
                null
        );

        List<Transacao> transacoes = transacaoRepository
                .listarComFiltros(filtros, Pageable.unpaged())
                .getContent();

        if (transacoes.isEmpty()) {
            return new Resultado(
                    consulta.dataInicio(),
                    consulta.dataFim(),
                    new Money(BigDecimal.ZERO, Currency.getInstance("BRL")),
                    List.of()
            );
        }

        Map<UUID, List<Transacao>> porCategoria = new HashMap<>();
        for (Transacao t : transacoes) {
            porCategoria.computeIfAbsent(t.getCategoriaId(), k -> new ArrayList<>()).add(t);
        }

        List<ItemGastoPorCategoria> itens = porCategoria.entrySet().stream()
                .map(entry -> {
                    UUID categoriaId = entry.getKey();
                    List<Transacao> grupo = entry.getValue();
                    Money zero = new Money(BigDecimal.ZERO, grupo.get(0).getValor().moeda());
                    Money total = grupo.stream()
                            .map(Transacao::getValor)
                            .reduce(zero, Money::somar);
                    String nome;
                    if (categoriaId == null) {
                        nome = "Sem categoria";
                    } else {
                        nome = categoriaRepository.buscarPorId(categoriaId)
                                .map(c -> c.getNome())
                                .orElse("Categoria desconhecida");
                    }
                    return new ItemGastoPorCategoria(categoriaId, nome, total);
                })
                .sorted((a, b) -> b.totalGasto().valor().compareTo(a.totalGasto().valor()))
                .collect(Collectors.toList());

        Money zero = new Money(BigDecimal.ZERO, itens.get(0).totalGasto().moeda());
        Money totalGeral = itens.stream()
                .map(ItemGastoPorCategoria::totalGasto)
                .reduce(zero, Money::somar);

        return new Resultado(
                consulta.dataInicio(),
                consulta.dataFim(),
                totalGeral,
                itens
        );
    }
}
