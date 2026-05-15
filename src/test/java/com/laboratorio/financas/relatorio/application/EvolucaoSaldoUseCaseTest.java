package com.laboratorio.financas.relatorio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;

class EvolucaoSaldoUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 3, 31);

    private TransacaoRepository transacaoRepository;
    private EvolucaoSaldoUseCase useCase;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        useCase = new EvolucaoSaldoUseCase(transacaoRepository);
    }

    @Test
    void semTransacoesRetornaZerosParaTodosOsMeses() {
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        var resultado = useCase.executar(new EvolucaoSaldoUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.totalReceitas().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.saldoLiquido().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.evolucaoPorMes()).hasSize(3);
        resultado.evolucaoPorMes().forEach(mes -> {
            assertThat(mes.totalReceitas().valor()).isEqualByComparingTo("0.00");
            assertThat(mes.totalDespesas().valor()).isEqualByComparingTo("0.00");
            assertThat(mes.saldoLiquido().valor()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    void transferenciaParContabilizaDespesaEReceita() {
        // No modelo Fase 1, TRANSFERENCIA gera par DESPESA+RECEITA com transferGroupId.
        // EvolucaoSaldo soma todas as despesas e receitas independente de serem par de transferencia.
        UUID contaId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Transacao despesaTransferencia = new Transacao(
                TipoTransacao.DESPESA,
                new Money(new BigDecimal("500.00"), BRL),
                LocalDate.of(2026, 1, 10),
                "transferencia teste",
                contaId,
                null,
                null,
                com.laboratorio.financas.transacao.domain.StatusTransacao.CLEARED,
                null,
                java.util.List.of()
        );
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(despesaTransferencia)));

        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 31);
        var resultado = useCase.executar(new EvolucaoSaldoUseCase.Consulta(inicio, fim, null));

        // DESPESA de transferencia conta no total de despesas
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("500.00");
        assertThat(resultado.totalReceitas().valor()).isEqualByComparingTo("0.00");
    }

    @Test
    void receitasEDespesasSomadasseparadamenteComSaldoLiquido() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 31);
        UUID contaId = UUID.randomUUID();
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        receita(new BigDecimal("1000.00"), LocalDate.of(2026, 1, 5), contaId),
                        despesa(new BigDecimal("300.00"), LocalDate.of(2026, 1, 10), contaId),
                        despesa(new BigDecimal("200.00"), LocalDate.of(2026, 1, 20), contaId)
                )));

        var resultado = useCase.executar(new EvolucaoSaldoUseCase.Consulta(inicio, fim, null));

        assertThat(resultado.totalReceitas().valor()).isEqualByComparingTo("1000.00");
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("500.00");
        assertThat(resultado.saldoLiquido().valor()).isEqualByComparingTo("500.00");
        assertThat(resultado.evolucaoPorMes()).hasSize(1);
        var mes = resultado.evolucaoPorMes().get(0);
        assertThat(mes.mes()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(mes.totalReceitas().valor()).isEqualByComparingTo("1000.00");
        assertThat(mes.totalDespesas().valor()).isEqualByComparingTo("500.00");
        assertThat(mes.saldoLiquido().valor()).isEqualByComparingTo("500.00");
    }

    @Test
    void transacoesAgrupadadasPorMesCorretamente() {
        UUID contaId = UUID.randomUUID();
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        receita(new BigDecimal("500.00"), LocalDate.of(2026, 1, 15), contaId),
                        receita(new BigDecimal("800.00"), LocalDate.of(2026, 2, 10), contaId),
                        despesa(new BigDecimal("100.00"), LocalDate.of(2026, 2, 20), contaId)
                )));

        var resultado = useCase.executar(new EvolucaoSaldoUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.evolucaoPorMes()).hasSize(3);
        var jan = resultado.evolucaoPorMes().get(0);
        assertThat(jan.mes()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(jan.totalReceitas().valor()).isEqualByComparingTo("500.00");

        var fev = resultado.evolucaoPorMes().get(1);
        assertThat(fev.mes()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(fev.totalReceitas().valor()).isEqualByComparingTo("800.00");
        assertThat(fev.totalDespesas().valor()).isEqualByComparingTo("100.00");
        assertThat(fev.saldoLiquido().valor()).isEqualByComparingTo("700.00");

        var mar = resultado.evolucaoPorMes().get(2);
        assertThat(mar.mes()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(mar.totalReceitas().valor()).isEqualByComparingTo("0.00");
    }

    @Test
    void filtroPassaTipoNuloContaIdEDatas() {
        UUID contaId = UUID.randomUUID();
        var captor = ArgumentCaptor.forClass(FiltrosTransacao.class);
        when(transacaoRepository.listarComFiltros(captor.capture(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        useCase.executar(new EvolucaoSaldoUseCase.Consulta(INICIO, FIM, contaId));

        var filtros = captor.getValue();
        assertThat(filtros.contaId()).isEqualTo(contaId);
        assertThat(filtros.tipo()).isNull();
        assertThat(filtros.dataInicio()).isEqualTo(INICIO);
        assertThat(filtros.dataFim()).isEqualTo(FIM);
    }

    @Test
    void totaisGeraisSomadosDeTodosOsMeses() {
        UUID contaId = UUID.randomUUID();
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        receita(new BigDecimal("1000.00"), LocalDate.of(2026, 1, 5), contaId),
                        receita(new BigDecimal("2000.00"), LocalDate.of(2026, 2, 5), contaId),
                        despesa(new BigDecimal("300.00"), LocalDate.of(2026, 1, 10), contaId),
                        despesa(new BigDecimal("400.00"), LocalDate.of(2026, 2, 10), contaId)
                )));

        var resultado = useCase.executar(new EvolucaoSaldoUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.totalReceitas().valor()).isEqualByComparingTo("3000.00");
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("700.00");
        assertThat(resultado.saldoLiquido().valor()).isEqualByComparingTo("2300.00");
    }

    private Transacao receita(BigDecimal valor, LocalDate data, UUID contaId) {
        return new Transacao(TipoTransacao.RECEITA, new Money(valor, BRL), data,
                "receita teste", contaId, null, null,
                com.laboratorio.financas.transacao.domain.StatusTransacao.CLEARED, null,
                java.util.List.of());
    }

    private Transacao despesa(BigDecimal valor, LocalDate data, UUID contaId) {
        return new Transacao(TipoTransacao.DESPESA, new Money(valor, BRL), data,
                "despesa teste", contaId, null, null,
                com.laboratorio.financas.transacao.domain.StatusTransacao.CLEARED, null,
                java.util.List.of());
    }
}
