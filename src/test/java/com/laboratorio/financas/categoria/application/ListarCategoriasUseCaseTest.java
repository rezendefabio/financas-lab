package com.laboratorio.financas.categoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarCategoriasUseCaseTest {

    private CategoriaRepository repository;
    private ListarCategoriasUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CategoriaRepository.class);
        useCase = new ListarCategoriasUseCase(repository);
    }

    @Test
    void executarComTipoNullDelegaParaListarTodas() {
        // Given
        when(repository.listarTodas()).thenReturn(List.of());

        // When
        useCase.executar(null);

        // Then
        verify(repository).listarTodas();
    }

    @Test
    void executarComTipoReceitaDelegaParaListarPorTipo() {
        // Given
        when(repository.listarPorTipo(TipoCategoria.RECEITA)).thenReturn(List.of());

        // When
        useCase.executar(TipoCategoria.RECEITA);

        // Then
        verify(repository).listarPorTipo(TipoCategoria.RECEITA);
    }

    @Test
    void executarComTipoDespesaDelegaParaListarPorTipo() {
        // Given
        when(repository.listarPorTipo(TipoCategoria.DESPESA)).thenReturn(List.of());

        // When
        useCase.executar(TipoCategoria.DESPESA);

        // Then
        verify(repository).listarPorTipo(TipoCategoria.DESPESA);
    }

    @Test
    void executarRetornaListaVazia() {
        // Given
        when(repository.listarTodas()).thenReturn(List.of());

        // When
        List<Categoria> resultado = useCase.executar(null);

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void executarRetornaListaComMultiplasCategorias() {
        // Given
        Categoria c1 = new Categoria("Salario", TipoCategoria.RECEITA);
        Categoria c2 = new Categoria("Aluguel", TipoCategoria.DESPESA);
        when(repository.listarTodas()).thenReturn(List.of(c1, c2));

        // When
        List<Categoria> resultado = useCase.executar(null);

        // Then
        assertThat(resultado).hasSize(2);
    }
}
