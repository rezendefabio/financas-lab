package com.laboratorio.financas.categoria.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeletarCategoriaUseCaseTest {

    private CategoriaRepository repository;
    private DeletarCategoriaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CategoriaRepository.class);
        useCase = new DeletarCategoriaUseCase(repository);
    }

    @Test
    void executarCaminhoFelizBuscaEDeletaCategoria() {
        // Given
        UUID id = UUID.randomUUID();
        Categoria categoria = new Categoria("Salario", TipoCategoria.RECEITA);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(categoria));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
        verify(repository, times(1)).deletar(id);
    }

    @Test
    void executarLancaCategoriaNaoEncontradaExceptionQuandoIdNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void executarNaoDeletaQuandoCategoriaNaoEncontrada() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When
        try {
            useCase.executar(id);
        } catch (CategoriaNaoEncontradaException e) {
            // esperado
        }

        // Then
        verify(repository, never()).deletar(id);
    }

    @Test
    void executarChamaRepositorioBuscarPorIdUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        Categoria categoria = new Categoria("Aluguel", TipoCategoria.DESPESA);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(categoria));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
    }

    @Test
    void executarLancaExcecaoComIdCorreto() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(CategoriaNaoEncontradaException.class)
                .satisfies(ex -> {
                    CategoriaNaoEncontradaException cnee = (CategoriaNaoEncontradaException) ex;
                    org.assertj.core.api.Assertions.assertThat(cnee.getId()).isEqualTo(id);
                });
    }
}
