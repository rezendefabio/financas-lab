package com.laboratorio.financas.centrocusto.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarCentrosCustoUseCaseTest {

    private CentroCustoRepository repository;
    private ListarCentrosCustoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CentroCustoRepository.class);
        useCase = new ListarCentrosCustoUseCase(repository);
    }

    @Test
    void executarRetornaCentrosDoUsuario() {
        UUID userId = UUID.randomUUID();
        CentroCusto cc1 = new CentroCusto(userId, "Casa", null);
        CentroCusto cc2 = new CentroCusto(userId, "Trabalho", null);
        when(repository.findByUserId(userId)).thenReturn(List.of(cc1, cc2));

        List<CentroCusto> resultado = useCase.executar(userId);

        assertThat(resultado).hasSize(2);
        verify(repository, times(1)).findByUserId(userId);
    }

    @Test
    void executarRetornaListaVaziaQuandoSemRegistros() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(List.of());

        List<CentroCusto> resultado = useCase.executar(userId);

        assertThat(resultado).isEmpty();
    }
}
