package com.laboratorio.financas.categoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Salario", TipoCategoria.RECEITA, null);

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
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Aluguel", TipoCategoria.DESPESA, null);

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
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Freelance", TipoCategoria.RECEITA, null);

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
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Mercado", TipoCategoria.DESPESA, null);

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
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Investimento", TipoCategoria.RECEITA, null);

        // When
        Categoria resultado = useCase.executar(comando);

        // Then
        assertThat(resultado.getTipo()).isEqualTo(TipoCategoria.RECEITA);
    }

    @Test
    void executarComCategoriaPaiValidaCriaSubcategoria() {
        // Given
        UUID paiId = UUID.randomUUID();
        Categoria pai = new Categoria("Alimentacao", TipoCategoria.DESPESA, null);
        Categoria filha = new Categoria("Mercado", TipoCategoria.DESPESA, paiId);
        when(repository.buscarPorId(paiId)).thenReturn(Optional.of(pai));
        when(repository.salvar(any(Categoria.class))).thenReturn(filha);
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Mercado", TipoCategoria.DESPESA, paiId);

        // When
        Categoria resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isSameAs(filha);
        verify(repository, times(1)).buscarPorId(paiId);
    }

    @Test
    void executarComCategoriaPaiInexistenteLancaException() {
        // Given
        UUID paiIdInexistente = UUID.randomUUID();
        when(repository.buscarPorId(paiIdInexistente)).thenReturn(Optional.empty());
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Sub", TipoCategoria.DESPESA, paiIdInexistente);

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void executarComCategoriaPaiQueESubcategoriaLancaIllegalArgument() {
        // Given
        UUID avoPaiId = UUID.randomUUID();
        UUID paiId = UUID.randomUUID();
        Categoria pai = new Categoria(paiId, "Sub", TipoCategoria.DESPESA, avoPaiId, java.time.Instant.now(), null);
        when(repository.buscarPorId(paiId)).thenReturn(Optional.of(pai));
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando("Neto", TipoCategoria.DESPESA, paiId);

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.executar(comando))
                .withMessageContaining("subcategoria de subcategoria");
    }
}
