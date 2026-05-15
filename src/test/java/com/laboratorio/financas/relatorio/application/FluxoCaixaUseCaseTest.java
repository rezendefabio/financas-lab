package com.laboratorio.financas.relatorio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;

class FluxoCaixaUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private TransacaoRepository transacaoRepository;
    private FluxoCaixaUseCase useCase;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        useCase = new FluxoCaixaUseCase(transacaoRepository);
    }

    @Test
    void mesSemTransacoesRetornaZerado() {
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        var resultado = useCase.executar(2026, 1);

        assertThat(resultado.ano()).isEqualTo(2026);
        assertThat(resultado.mes()).isEqualTo(1);
        assertThat(resultado.totalReceitas()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalDespesas()).isEqualByComparingTo("0.00");
        assertThat(resultado.saldo()).isEqualByComparingTo("0.00");
        assertThat(resultado.moeda()).isEqualTo("BRL");
    }

    @Test
    void mesSoComReceitasRetornaSaldoPositivo() {
        Transacao receita1 = transacao(TipoTransacao.RECEITA, new BigDecimal("1000.00"));
        Transacao receita2 = transacao(TipoTransacao.RECEITA, new BigDecimal("500.00"));
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receita1, receita2)));

        var resultado = useCase.executar(2026, 3);

        assertThat(resultado.totalReceitas()).isEqualByComparingTo("1500.00");
        assertThat(resultado.totalDespesas()).isEqualByComparingTo("0.00");
        assertThat(resultado.saldo()).isEqualByComparingTo("1500.00");
    }

    @Test
    void mesSoComDespesasRetornaSaldoNegativo() {
        Transacao despesa1 = transacao(TipoTransacao.DESPESA, new BigDecimal("300.00"));
        Transacao despesa2 = transacao(TipoTransacao.DESPESA, new BigDecimal("200.00"));
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(despesa1, despesa2)));

        var resultado = useCase.executar(2026, 4);

        assertThat(resultado.totalReceitas()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalDespesas()).isEqualByComparingTo("500.00");
        assertThat(resultado.saldo()).isEqualByComparingTo("-500.00");
    }

    @Test
    void mesMistoCalculaSaldoCorretamente() {
        Transacao receita = transacao(TipoTransacao.RECEITA, new BigDecimal("2000.00"));
        Transacao despesa = transacao(TipoTransacao.DESPESA, new BigDecimal("800.00"));
        Transacao transferencia = transacao(TipoTransacao.TRANSFERENCIA, new BigDecimal("100.00"));
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receita, despesa, transferencia)));

        var resultado = useCase.executar(2026, 5);

        assertThat(resultado.totalReceitas()).isEqualByComparingTo("2000.00");
        assertThat(resultado.totalDespesas()).isEqualByComparingTo("800.00");
        // transferencia nao conta para receita nem despesa
        assertThat(resultado.saldo()).isEqualByComparingTo("1200.00");
    }

    private Transacao transacao(TipoTransacao tipo, BigDecimal valor) {
        if (tipo == TipoTransacao.TRANSFERENCIA) {
            UUID contaId = UUID.randomUUID();
            UUID contaDestinoId = UUID.randomUUID();
            return new Transacao(
                    tipo,
                    new Money(valor, BRL),
                    LocalDate.of(2026, 1, 10),
                    "transferencia teste",
                    contaId,
                    contaDestinoId,
                    null
            );
        }
        return new Transacao(
                tipo,
                new Money(valor, BRL),
                LocalDate.of(2026, 1, 10),
                "transacao teste",
                UUID.randomUUID(),
                null,
                null
        );
    }
}
