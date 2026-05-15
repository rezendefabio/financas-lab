package com.laboratorio.financas.relatorio.application;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FluxoCaixaUseCase {

    private final TransacaoRepository transacaoRepository;

    public FluxoCaixaUseCase(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public record FluxoCaixaResponse(
            int ano,
            int mes,
            BigDecimal totalReceitas,
            BigDecimal totalDespesas,
            BigDecimal saldo,
            String moeda
    ) { }

    @Transactional(readOnly = true)
    public FluxoCaixaResponse executar(int ano, int mes, UUID userId) {
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        FiltrosTransacao filtros = new FiltrosTransacao(null, inicio, fim, null, null, userId);

        List<Transacao> transacoes = transacaoRepository
                .listarComFiltros(filtros, Pageable.unpaged())
                .getContent();

        BigDecimal totalReceitas = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.RECEITA)
                .map(t -> t.getValor().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDespesas = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .map(t -> t.getValor().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldo = totalReceitas.subtract(totalDespesas);

        return new FluxoCaixaResponse(ano, mes, totalReceitas, totalDespesas, saldo, "BRL");
    }
}
