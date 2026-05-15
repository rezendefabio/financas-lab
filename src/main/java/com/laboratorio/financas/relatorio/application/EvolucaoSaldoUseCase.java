package com.laboratorio.financas.relatorio.application;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EvolucaoSaldoUseCase {

    private final TransacaoRepository transacaoRepository;

    public EvolucaoSaldoUseCase(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public record Consulta(LocalDate dataInicio, LocalDate dataFim, UUID contaId, UUID userId) { }

    public record ItemEvolucaoMes(
            LocalDate mes,
            Money totalReceitas,
            Money totalDespesas,
            Money saldoLiquido
    ) { }

    public record Resultado(
            LocalDate dataInicio,
            LocalDate dataFim,
            Money totalReceitas,
            Money totalDespesas,
            Money saldoLiquido,
            List<ItemEvolucaoMes> evolucaoPorMes
    ) {
        public Resultado {
            evolucaoPorMes = List.copyOf(evolucaoPorMes);
        }
    }

    @Transactional(readOnly = true)
    public Resultado executar(Consulta consulta) {
        FiltrosTransacao filtros = new FiltrosTransacao(
                consulta.contaId(),
                consulta.dataInicio(),
                consulta.dataFim(),
                null,
                null,
                consulta.userId()
        );

        List<Transacao> transacoes = transacaoRepository
                .listarComFiltros(filtros, Pageable.unpaged())
                .getContent();

        Currency brl = Currency.getInstance("BRL");
        Money zeroBrl = new Money(BigDecimal.ZERO, brl);

        Map<YearMonth, List<Transacao>> porMes = new HashMap<>();
        for (Transacao t : transacoes) {
            porMes.computeIfAbsent(YearMonth.from(t.getData()), k -> new ArrayList<>()).add(t);
        }

        YearMonth mesAtual = YearMonth.from(consulta.dataInicio());
        YearMonth mesFim = YearMonth.from(consulta.dataFim());
        List<ItemEvolucaoMes> evolucaoPorMes = new ArrayList<>();

        while (!mesAtual.isAfter(mesFim)) {
            List<Transacao> transacoesDomes = porMes.getOrDefault(mesAtual, List.of());

            Money totalReceitas = transacoesDomes.stream()
                    .filter(t -> t.getTipo() == TipoTransacao.RECEITA)
                    .map(Transacao::getValor)
                    .reduce(zeroBrl, Money::somar);

            Money totalDespesas = transacoesDomes.stream()
                    .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                    .map(Transacao::getValor)
                    .reduce(zeroBrl, Money::somar);

            Money saldoLiquido = totalReceitas.subtrair(totalDespesas);

            evolucaoPorMes.add(new ItemEvolucaoMes(
                    mesAtual.atDay(1),
                    totalReceitas,
                    totalDespesas,
                    saldoLiquido
            ));

            mesAtual = mesAtual.plusMonths(1);
        }

        Money totalReceitas = evolucaoPorMes.stream()
                .map(ItemEvolucaoMes::totalReceitas)
                .reduce(zeroBrl, Money::somar);

        Money totalDespesas = evolucaoPorMes.stream()
                .map(ItemEvolucaoMes::totalDespesas)
                .reduce(zeroBrl, Money::somar);

        Money saldoLiquido = totalReceitas.subtrair(totalDespesas);

        return new Resultado(
                consulta.dataInicio(),
                consulta.dataFim(),
                totalReceitas,
                totalDespesas,
                saldoLiquido,
                evolucaoPorMes
        );
    }
}
