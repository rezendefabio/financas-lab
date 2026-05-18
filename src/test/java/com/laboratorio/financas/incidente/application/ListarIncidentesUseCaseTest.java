package com.laboratorio.financas.incidente.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.ErroRegistradoRepository;
import com.laboratorio.financas.incidente.domain.FiltrosIncidente;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarIncidentesUseCaseTest {

    private ErroRegistradoRepository erroRegistradoRepository;
    private ListarIncidentesUseCase useCase;

    @BeforeEach
    void setUp() {
        erroRegistradoRepository = Mockito.mock(ErroRegistradoRepository.class);
        useCase = new ListarIncidentesUseCase(erroRegistradoRepository);
    }

    @Test
    void executarDelegaFiltrosAoRepositorioERetornaLista() {
        // Given
        FiltrosIncidente filtros = new FiltrosIncidente(null, null, "RuntimeException", null);
        ErroRegistrado erro = new ErroRegistrado("op", "RuntimeException", "msg", "stack");
        when(erroRegistradoRepository.listarComFiltros(filtros)).thenReturn(List.of(erro));

        // When
        List<ErroRegistrado> resultado = useCase.executar(filtros);

        // Then
        assertThat(resultado).containsExactly(erro);
        verify(erroRegistradoRepository).listarComFiltros(filtros);
    }

    @Test
    void executarRetornaListaVaziaQuandoRepositorioNaoEncontraNada() {
        // Given
        FiltrosIncidente filtros = new FiltrosIncidente(null, null, null, null);
        when(erroRegistradoRepository.listarComFiltros(any())).thenReturn(List.of());

        // When
        List<ErroRegistrado> resultado = useCase.executar(filtros);

        // Then
        assertThat(resultado).isEmpty();
    }
}
