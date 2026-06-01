package com.laboratorio.financas.centrocusto.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoNaoEncontradoException;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarCentroCustoPorIdUseCaseTest {

    private CentroCustoRepository repository;
    private BuscarCentroCustoPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CentroCustoRepository.class);
        useCase = new BuscarCentroCustoPorIdUseCase(repository);
    }

    @Test
    void executarRetornaCentroCustoQuandoExiste() {
        UUID userId = UUID.randomUUID();
        CentroCusto cc = new CentroCusto(userId, "Casa", null);
        when(repository.findById(cc.getId())).thenReturn(Optional.of(cc));

        CentroCusto resultado = useCase.executar(cc.getId());

        assertThat(resultado).isSameAs(cc);
    }

    @Test
    void executarLancaExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(CentroCustoNaoEncontradoException.class);
    }
}
