package com.laboratorio.financas.transacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.OrdenacaoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ListarTransacoesUseCaseTest {

    private TransacaoRepository repository;
    private ListarTransacoesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TransacaoRepository.class);
        useCase = new ListarTransacoesUseCase(repository);
    }

    @Test
    void executarDelegaAoRepositorioComFiltrosOrdenacaoEPaginacao() {
        // Given
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);
        Page<Transacao> paginaVazia = Page.empty(PageRequest.of(0, 20));
        when(repository.listarComFiltrosOrdenado(
                filtros, 0, 20, OrdenacaoTransacao.DATA, Sort.Direction.DESC))
                .thenReturn(paginaVazia);

        // When
        Page<Transacao> resultado = useCase.executar(
                filtros, 0, 20, OrdenacaoTransacao.DATA, Sort.Direction.DESC);

        // Then
        assertThat(resultado).isNotNull();
        verify(repository, times(1)).listarComFiltrosOrdenado(
                filtros, 0, 20, OrdenacaoTransacao.DATA, Sort.Direction.DESC);
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        // Given
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);
        Page<Transacao> paginaEsperada = Page.empty(PageRequest.of(0, 20));
        when(repository.listarComFiltrosOrdenado(
                filtros, 0, 20, OrdenacaoTransacao.DATA, Sort.Direction.DESC))
                .thenReturn(paginaEsperada);

        // When
        Page<Transacao> resultado = useCase.executar(
                filtros, 0, 20, OrdenacaoTransacao.DATA, Sort.Direction.DESC);

        // Then
        assertThat(resultado).isSameAs(paginaEsperada);
    }

    @Test
    void executarComFiltroContaIdPassaFiltroAoRepositorio() {
        // Given
        UUID contaId = UUID.randomUUID();
        FiltrosTransacao filtros = new FiltrosTransacao(contaId, null, null, null, null);
        when(repository.listarComFiltrosOrdenado(
                eq(filtros), anyInt(), anyInt(),
                any(OrdenacaoTransacao.class), any(Sort.Direction.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        // When
        useCase.executar(filtros, 0, 20, OrdenacaoTransacao.VALOR, Sort.Direction.ASC);

        // Then
        verify(repository, times(1)).listarComFiltrosOrdenado(
                eq(filtros), anyInt(), anyInt(),
                any(OrdenacaoTransacao.class), any(Sort.Direction.class));
    }

    @Test
    void executarComFiltrosTipoEPeriodoPassaFiltroAoRepositorio() {
        // Given
        FiltrosTransacao filtros = new FiltrosTransacao(
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31),
                TipoTransacao.RECEITA,
                null
        );
        when(repository.listarComFiltrosOrdenado(
                eq(filtros), anyInt(), anyInt(),
                any(OrdenacaoTransacao.class), any(Sort.Direction.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        // When
        useCase.executar(filtros, 0, 10, OrdenacaoTransacao.DATA, Sort.Direction.DESC);

        // Then
        verify(repository, times(1)).listarComFiltrosOrdenado(
                eq(filtros), anyInt(), anyInt(),
                any(OrdenacaoTransacao.class), any(Sort.Direction.class));
    }

    @Test
    void executarPropagaOrdenacaoEDirecaoAoRepositorio() {
        // Given
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);
        when(repository.listarComFiltrosOrdenado(
                any(FiltrosTransacao.class), anyInt(), anyInt(),
                any(OrdenacaoTransacao.class), any(Sort.Direction.class)))
                .thenReturn(Page.empty(PageRequest.of(1, 5)));

        // When
        useCase.executar(filtros, 1, 5, OrdenacaoTransacao.STATUS, Sort.Direction.ASC);

        // Then
        verify(repository, times(1)).listarComFiltrosOrdenado(
                filtros, 1, 5, OrdenacaoTransacao.STATUS, Sort.Direction.ASC);
    }
}
