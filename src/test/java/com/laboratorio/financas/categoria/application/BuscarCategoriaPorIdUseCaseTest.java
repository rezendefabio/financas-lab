package com.laboratorio.financas.categoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarCategoriaPorIdUseCaseTest {

    private CategoriaRepository repository;
    private BuscarCategoriaPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CategoriaRepository.class);
        useCase = new BuscarCategoriaPorIdUseCase(repository);
    }

    @Test
    void executarRetornaCategoriaQuandoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        Categoria categoria = new Categoria("Salario", TipoCategoria.RECEITA);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(categoria));

        // When
        Categoria resultado = useCase.executar(id);

        // Then
        assertThat(resultado).isSameAs(categoria);
    }

    @Test
    void executarLancaCategoriaNaoEncontradaExceptionComIdCorretoQuandoNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(CategoriaNaoEncontradaException.class)
                .satisfies(ex -> {
                    CategoriaNaoEncontradaException cnee = (CategoriaNaoEncontradaException) ex;
                    assertThat(cnee.getId()).isEqualTo(id);
                });
    }

    @Test
    void executarChamaRepositorioUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        Categoria categoria = new Categoria("Aluguel", TipoCategoria.DESPESA);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(categoria));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
    }
}
