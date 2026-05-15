package com.laboratorio.financas.instituicao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.domain.InstituicaoNaoEncontradaException;
import com.laboratorio.financas.instituicao.domain.InstituicaoRepository;
import com.laboratorio.financas.instituicao.domain.TipoInstituicao;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarInstituicaoPorIdUseCaseTest {

    private InstituicaoRepository repository;
    private BuscarInstituicaoPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(InstituicaoRepository.class);
        useCase = new BuscarInstituicaoPorIdUseCase(repository);
    }

    @Test
    void executarRetornaInstituicaoQuandoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        Instituicao inst = new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true);
        when(repository.findById(id)).thenReturn(Optional.of(inst));

        // When
        Instituicao resultado = useCase.executar(id);

        // Then
        assertThat(resultado).isSameAs(inst);
    }

    @Test
    void executarLancaInstituicaoNaoEncontradaExceptionComIdCorretoQuandoNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(InstituicaoNaoEncontradaException.class)
                .satisfies(ex -> {
                    InstituicaoNaoEncontradaException inee = (InstituicaoNaoEncontradaException) ex;
                    assertThat(inee.getId()).isEqualTo(id);
                });
    }

    @Test
    void executarChamaRepositorioUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        Instituicao inst = new Instituicao("Bradesco", "237", TipoInstituicao.BANCO_TRADICIONAL, null, true);
        when(repository.findById(id)).thenReturn(Optional.of(inst));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).findById(id);
    }
}
