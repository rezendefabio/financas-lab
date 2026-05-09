package com.laboratorio.financas.transacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
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
import org.springframework.data.domain.Pageable;

class ListarTransacoesUseCaseTest {

    private TransacaoRepository repository;
    private ListarTransacoesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TransacaoRepository.class);
        useCase = new ListarTransacoesUseCase(repository);
    }

    @Test
    void executarDelegaAoRepositorioComFiltrosEPageable() {
        // Given
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transacao> paginaVazia = Page.empty(pageable);
        when(repository.listarComFiltros(filtros, pageable)).thenReturn(paginaVazia);

        // When
        Page<Transacao> resultado = useCase.executar(filtros, pageable);

        // Then
        assertThat(resultado).isNotNull();
        verify(repository, times(1)).listarComFiltros(filtros, pageable);
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        // Given
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transacao> paginaEsperada = Page.empty(pageable);
        when(repository.listarComFiltros(filtros, pageable)).thenReturn(paginaEsperada);

        // When
        Page<Transacao> resultado = useCase.executar(filtros, pageable);

        // Then
        assertThat(resultado).isSameAs(paginaEsperada);
    }

    @Test
    void executarComFiltroContaIdPassaFiltroAoRepositorio() {
        // Given
        UUID contaId = UUID.randomUUID();
        FiltrosTransacao filtros = new FiltrosTransacao(contaId, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transacao> pagina = Page.empty(pageable);
        when(repository.listarComFiltros(eq(filtros), any(Pageable.class))).thenReturn(pagina);

        // When
        useCase.executar(filtros, pageable);

        // Then
        verify(repository, times(1)).listarComFiltros(eq(filtros), any(Pageable.class));
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
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transacao> pagina = Page.empty(pageable);
        when(repository.listarComFiltros(eq(filtros), any(Pageable.class))).thenReturn(pagina);

        // When
        useCase.executar(filtros, pageable);

        // Then
        verify(repository, times(1)).listarComFiltros(eq(filtros), any(Pageable.class));
    }

    @Test
    void executarChamaRepositorioUmaVez() {
        // Given
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);
        Pageable pageable = PageRequest.of(1, 5);
        when(repository.listarComFiltros(any(FiltrosTransacao.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        // When
        useCase.executar(filtros, pageable);

        // Then
        verify(repository, times(1)).listarComFiltros(any(FiltrosTransacao.class), any(Pageable.class));
    }
}
