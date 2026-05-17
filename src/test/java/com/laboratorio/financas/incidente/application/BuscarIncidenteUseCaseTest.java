package com.laboratorio.financas.incidente.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.ErroRegistradoRepository;
import com.laboratorio.financas.incidente.domain.IncidenteNaoEncontradoException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarIncidenteUseCaseTest {

    private ErroRegistradoRepository erroRegistradoRepository;
    private BuscarIncidenteUseCase useCase;

    @BeforeEach
    void setUp() {
        erroRegistradoRepository = Mockito.mock(ErroRegistradoRepository.class);
        useCase = new BuscarIncidenteUseCase(erroRegistradoRepository);
    }

    @Test
    void executarRetornaErroQuandoCodigoExiste() {
        // Given
        ErroRegistrado erro = new ErroRegistrado("op", "Classe", "msg", "stack");
        when(erroRegistradoRepository.buscarPorCodigo(erro.getCodigo()))
                .thenReturn(Optional.of(erro));

        // When
        ErroRegistrado resultado = useCase.executar(erro.getCodigo());

        // Then
        assertThat(resultado).isSameAs(erro);
    }

    @Test
    void executarLancaExcecaoQuandoCodigoNaoExiste() {
        // Given
        when(erroRegistradoRepository.buscarPorCodigo("ERR-99999999"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar("ERR-99999999"))
                .isInstanceOf(IncidenteNaoEncontradoException.class)
                .satisfies(ex -> assertThat(((IncidenteNaoEncontradoException) ex).getCodigo())
                        .isEqualTo("ERR-99999999"));
    }
}
