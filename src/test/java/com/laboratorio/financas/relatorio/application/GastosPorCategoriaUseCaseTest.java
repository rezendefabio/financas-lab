package com.laboratorio.financas.relatorio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
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

class GastosPorCategoriaUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 1, 31);

    private TransacaoRepository transacaoRepository;
    private CategoriaRepository categoriaRepository;
    private GastosPorCategoriaUseCase useCase;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        categoriaRepository = Mockito.mock(CategoriaRepository.class);
        useCase = new GastosPorCategoriaUseCase(transacaoRepository, categoriaRepository);
    }

    @Test
    void semTransacoesRetornaResultadoComZeroEListaVazia() {
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        var resultado = useCase.executar(new GastosPorCategoriaUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.totalGeral().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.itensPorCategoria()).isEmpty();
        assertThat(resultado.dataInicio()).isEqualTo(INICIO);
        assertThat(resultado.dataFim()).isEqualTo(FIM);
    }

    @Test
    void transacaoSemCategoriaRetornaNomeSemCategoria() {
        Transacao transacao = despesa(new BigDecimal("100.00"), null);
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(transacao)));

        var resultado = useCase.executar(new GastosPorCategoriaUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.itensPorCategoria()).hasSize(1);
        var item = resultado.itensPorCategoria().get(0);
        assertThat(item.categoriaId()).isNull();
        assertThat(item.nomeCategoria()).isEqualTo("Sem categoria");
        assertThat(item.totalGasto().valor()).isEqualByComparingTo("100.00");
    }

    @Test
    void transacaoComCategoriaEncontradaRetornaNomeDaCategoria() {
        UUID categoriaId = UUID.randomUUID();
        Transacao transacao = despesa(new BigDecimal("200.00"), categoriaId);
        Categoria categoria = new Categoria("Alimentacao", TipoCategoria.DESPESA);
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(transacao)));
        when(categoriaRepository.buscarPorId(categoriaId))
                .thenReturn(Optional.of(categoria));

        var resultado = useCase.executar(new GastosPorCategoriaUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.itensPorCategoria()).hasSize(1);
        assertThat(resultado.itensPorCategoria().get(0).nomeCategoria()).isEqualTo("Alimentacao");
    }

    @Test
    void transacaoComCategoriaDesconhecidaRetornaFallback() {
        UUID categoriaId = UUID.randomUUID();
        Transacao transacao = despesa(new BigDecimal("150.00"), categoriaId);
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(transacao)));
        when(categoriaRepository.buscarPorId(categoriaId)).thenReturn(Optional.empty());

        var resultado = useCase.executar(new GastosPorCategoriaUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.itensPorCategoria().get(0).nomeCategoria())
                .isEqualTo("Categoria desconhecida");
    }

    @Test
    void multiplosGruposOrdenadosPorTotalDecrescente() {
        UUID cat1 = UUID.randomUUID();
        UUID cat2 = UUID.randomUUID();
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        despesa(new BigDecimal("50.00"), cat1),
                        despesa(new BigDecimal("200.00"), cat2),
                        despesa(new BigDecimal("30.00"), cat1)
                )));
        when(categoriaRepository.buscarPorId(cat1))
                .thenReturn(Optional.of(new Categoria("Transporte", TipoCategoria.DESPESA)));
        when(categoriaRepository.buscarPorId(cat2))
                .thenReturn(Optional.of(new Categoria("Alimentacao", TipoCategoria.DESPESA)));

        var resultado = useCase.executar(new GastosPorCategoriaUseCase.Consulta(INICIO, FIM, null));

        assertThat(resultado.itensPorCategoria()).hasSize(2);
        assertThat(resultado.itensPorCategoria().get(0).totalGasto().valor())
                .isEqualByComparingTo("200.00");
        assertThat(resultado.itensPorCategoria().get(1).totalGasto().valor())
                .isEqualByComparingTo("80.00");
        assertThat(resultado.totalGeral().valor()).isEqualByComparingTo("280.00");
    }

    @Test
    void filtroPassaContaIdETipoDespesa() {
        UUID contaId = UUID.randomUUID();
        when(transacaoRepository.listarComFiltros(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        var captor = ArgumentCaptor.forClass(
                com.laboratorio.financas.transacao.domain.FiltrosTransacao.class);
        when(transacaoRepository.listarComFiltros(captor.capture(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        useCase.executar(new GastosPorCategoriaUseCase.Consulta(INICIO, FIM, contaId));

        var filtros = captor.getValue();
        assertThat(filtros.contaId()).isEqualTo(contaId);
        assertThat(filtros.tipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(filtros.dataInicio()).isEqualTo(INICIO);
        assertThat(filtros.dataFim()).isEqualTo(FIM);
    }

    private Transacao despesa(BigDecimal valor, UUID categoriaId) {
        return new Transacao(TipoTransacao.DESPESA, new Money(valor, BRL), INICIO,
                "despesa teste", UUID.randomUUID(), categoriaId, null,
                com.laboratorio.financas.transacao.domain.StatusTransacao.CLEARED, null,
                java.util.List.of());
    }
}
