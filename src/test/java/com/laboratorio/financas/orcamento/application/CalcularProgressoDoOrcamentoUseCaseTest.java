package com.laboratorio.financas.orcamento.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoNaoEncontradoException;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import com.laboratorio.financas.orcamento.domain.StatusProgresso;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;

class CalcularProgressoDoOrcamentoUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate MES = LocalDate.of(2026, 5, 1);

    private OrcamentoRepository orcamentoRepository;
    private TransacaoRepository transacaoRepository;
    private CalcularProgressoDoOrcamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        orcamentoRepository = Mockito.mock(OrcamentoRepository.class);
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        useCase = new CalcularProgressoDoOrcamentoUseCase(orcamentoRepository, transacaoRepository);
    }

    @Test
    void orcamentoNaoEncontradoLancaOrcamentoNaoEncontradoException() {
        UUID orcamentoId = UUID.randomUUID();
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(orcamentoId))
                .isInstanceOf(OrcamentoNaoEncontradoException.class)
                .satisfies(ex -> assertThat(((OrcamentoNaoEncontradoException) ex).getId())
                        .isEqualTo(orcamentoId));
    }

    @Test
    void zeroDespesasRetornaStatusAbaixoComPercentualZero() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = orcamento(orcamentoId, new BigDecimal("1000.00"));
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        CalcularProgressoDoOrcamentoUseCase.Resultado resultado = useCase.executar(orcamentoId);

        assertThat(resultado.status()).isEqualTo(StatusProgresso.ABAIXO);
        assertThat(resultado.percentualUtilizado()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalGasto().valor()).isEqualByComparingTo("0.00");
    }

    @Test
    void despesaAbaixoDe80PorcentoRetornaStatusAbaixo() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = orcamento(orcamentoId, new BigDecimal("1000.00"));
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        // 500 / 1000 = 50% -> ABAIXO
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        despesa(new BigDecimal("500.00"), orcamento.getCategoriaId()))));

        CalcularProgressoDoOrcamentoUseCase.Resultado resultado = useCase.executar(orcamentoId);

        assertThat(resultado.status()).isEqualTo(StatusProgresso.ABAIXO);
        assertThat(resultado.percentualUtilizado()).isEqualByComparingTo("50.00");
    }

    @Test
    void despesaExatamente80PorcentoRetornaStatusAtencao() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = orcamento(orcamentoId, new BigDecimal("1000.00"));
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        // 800 / 1000 = 80% -> fronteira ATENCAO
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        despesa(new BigDecimal("800.00"), orcamento.getCategoriaId()))));

        CalcularProgressoDoOrcamentoUseCase.Resultado resultado = useCase.executar(orcamentoId);

        assertThat(resultado.status()).isEqualTo(StatusProgresso.ATENCAO);
        assertThat(resultado.percentualUtilizado()).isEqualByComparingTo("80.00");
    }

    @Test
    void despesaEntre80E100PorcentoRetornaStatusAtencao() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = orcamento(orcamentoId, new BigDecimal("1000.00"));
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        // 900 / 1000 = 90% -> ATENCAO
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        despesa(new BigDecimal("900.00"), orcamento.getCategoriaId()))));

        CalcularProgressoDoOrcamentoUseCase.Resultado resultado = useCase.executar(orcamentoId);

        assertThat(resultado.status()).isEqualTo(StatusProgresso.ATENCAO);
        assertThat(resultado.percentualUtilizado()).isEqualByComparingTo("90.00");
    }

    @Test
    void despesaExatamente100PorcentoRetornaStatusAtingido() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = orcamento(orcamentoId, new BigDecimal("1000.00"));
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        // 1000 / 1000 = 100% -> fronteira ATINGIDO
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        despesa(new BigDecimal("1000.00"), orcamento.getCategoriaId()))));

        CalcularProgressoDoOrcamentoUseCase.Resultado resultado = useCase.executar(orcamentoId);

        assertThat(resultado.status()).isEqualTo(StatusProgresso.ATINGIDO);
        assertThat(resultado.percentualUtilizado()).isEqualByComparingTo("100.00");
    }

    @Test
    void despesaAcimaDe100PorcentoRetornaStatusExcedido() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = orcamento(orcamentoId, new BigDecimal("1000.00"));
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        // 1200 / 1000 = 120% -> EXCEDIDO
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        despesa(new BigDecimal("1200.00"), orcamento.getCategoriaId()))));

        CalcularProgressoDoOrcamentoUseCase.Resultado resultado = useCase.executar(orcamentoId);

        assertThat(resultado.status()).isEqualTo(StatusProgresso.EXCEDIDO);
        assertThat(resultado.percentualUtilizado()).isEqualByComparingTo("120.00");
    }

    @Test
    void listarComFiltrosChamadoComCategoriaIdTipoDespesaEDatasDoMes() {
        UUID orcamentoId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        Orcamento orcamento = orcamentoComCategoria(orcamentoId, new BigDecimal("1000.00"), categoriaId);
        when(orcamentoRepository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        ArgumentCaptor<FiltrosTransacao> filtrosCaptor = ArgumentCaptor.forClass(FiltrosTransacao.class);
        when(transacaoRepository.listarComFiltros(filtrosCaptor.capture(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        useCase.executar(orcamentoId);

        FiltrosTransacao filtros = filtrosCaptor.getValue();
        assertThat(filtros.categoriaId()).isEqualTo(categoriaId);
        assertThat(filtros.tipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(filtros.dataInicio()).isEqualTo(MES);
        assertThat(filtros.dataFim()).isEqualTo(MES.plusMonths(1).minusDays(1));
        assertThat(filtros.contaId()).isNull();
    }

    private Orcamento orcamento(UUID id, BigDecimal valorLimite) {
        return new Orcamento(id, UUID.randomUUID(), UUID.randomUUID(), new Money(valorLimite, BRL), MES,
                true, Instant.now(), Instant.now());
    }

    private Orcamento orcamentoComCategoria(UUID id, BigDecimal valorLimite, UUID categoriaId) {
        return new Orcamento(id, UUID.randomUUID(), categoriaId, new Money(valorLimite, BRL), MES,
                true, Instant.now(), Instant.now());
    }

    private Transacao despesa(BigDecimal valor, UUID categoriaId) {
        return new Transacao(TipoTransacao.DESPESA, new Money(valor, BRL), MES,
                "despesa teste", UUID.randomUUID(), categoriaId, null,
                com.laboratorio.financas.transacao.domain.StatusTransacao.CLEARED, null,
                java.util.List.of());
    }
}
