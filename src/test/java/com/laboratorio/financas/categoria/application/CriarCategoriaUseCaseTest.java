package com.laboratorio.financas.categoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarCategoriaUseCaseTest {

    private CategoriaRepository repository;
    private CriarCategoriaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CategoriaRepository.class);
        useCase = new CriarCategoriaUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaCategoria() {
        // Given
        Categoria categoriaSalva = new Categoria("Salario", TipoCategoria.RECEITA);
        when(repository.salvar(any(Categoria.class))).thenReturn(categoriaSalva);
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Salario", TipoCategoria.RECEITA);

        // When
        Categoria resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Salario");
    }

    @Test
    void executarChamaRepositorioSalvarUmaVez() {
        // Given
        Categoria categoriaSalva = new Categoria("Aluguel", TipoCategoria.DESPESA);
        when(repository.salvar(any(Categoria.class))).thenReturn(categoriaSalva);
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Aluguel", TipoCategoria.DESPESA);

        // When
        useCase.executar(comando);

        // Then
        verify(repository, times(1)).salvar(any(Categoria.class));
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        // Given
        Categoria categoriaSalva = new Categoria("Freelance", TipoCategoria.RECEITA);
        when(repository.salvar(any(Categoria.class))).thenReturn(categoriaSalva);
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Freelance", TipoCategoria.RECEITA);

        // When
        Categoria resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isSameAs(categoriaSalva);
    }

    @Test
    void executarComTipoDesesaRetornaCategoria() {
        // Given
        Categoria categoriaSalva = new Categoria("Mercado", TipoCategoria.DESPESA);
        when(repository.salvar(any(Categoria.class))).thenReturn(categoriaSalva);
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Mercado", TipoCategoria.DESPESA);

        // When
        Categoria resultado = useCase.executar(comando);

        // Then
        assertThat(resultado.getTipo()).isEqualTo(TipoCategoria.DESPESA);
    }

    @Test
    void executarComTipoReceitaRetornaCategoria() {
        // Given
        Categoria categoriaSalva = new Categoria("Investimento", TipoCategoria.RECEITA);
        when(repository.salvar(any(Categoria.class))).thenReturn(categoriaSalva);
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Investimento", TipoCategoria.RECEITA);

        // When
        Categoria resultado = useCase.executar(comando);

        // Then
        assertThat(resultado.getTipo()).isEqualTo(TipoCategoria.RECEITA);
    }
}
