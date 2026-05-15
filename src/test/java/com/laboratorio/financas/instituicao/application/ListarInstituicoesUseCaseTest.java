package com.laboratorio.financas.instituicao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.domain.InstituicaoRepository;
import com.laboratorio.financas.instituicao.domain.TipoInstituicao;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarInstituicoesUseCaseTest {

    private InstituicaoRepository repository;
    private ListarInstituicoesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(InstituicaoRepository.class);
        useCase = new ListarInstituicoesUseCase(repository);
    }

    @Test
    void executarRetornaListaDeInstituicoesAtivas() {
        // Given
        Instituicao nubank = new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true);
        Instituicao inter = new Instituicao("Inter", "077", TipoInstituicao.BANCO_DIGITAL, null, true);
        when(repository.findAllAtivas()).thenReturn(List.of(nubank, inter));

        // When
        List<Instituicao> resultado = useCase.executar();

        // Then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactlyInAnyOrder(nubank, inter);
    }

    @Test
    void executarRetornaListaVaziaQuandoNenhumaAtiva() {
        // Given
        when(repository.findAllAtivas()).thenReturn(List.of());

        // When
        List<Instituicao> resultado = useCase.executar();

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void executarChamaFindAllAtivasUmaVez() {
        // Given
        when(repository.findAllAtivas()).thenReturn(List.of());

        // When
        useCase.executar();

        // Then
        verify(repository, times(1)).findAllAtivas();
    }
}
