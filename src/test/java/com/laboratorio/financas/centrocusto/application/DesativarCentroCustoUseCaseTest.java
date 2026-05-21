package com.laboratorio.financas.centrocusto.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoNaoEncontradoException;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DesativarCentroCustoUseCaseTest {

    private CentroCustoRepository repository;
    private DesativarCentroCustoUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CentroCustoRepository.class);
        useCase = new DesativarCentroCustoUseCase(repository);
    }

    @Test
    void executarBuscaDesativaESalva() {
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", null);
        when(repository.findByIdAndUserId(cc.getId(), USER_ID)).thenReturn(Optional.of(cc));
        when(repository.save(any(CentroCusto.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.executar(cc.getId(), USER_ID);

        verify(repository, times(1)).findByIdAndUserId(cc.getId(), USER_ID);
        verify(repository, times(1)).save(argThat(updated -> !updated.isAtivo()));
    }

    @Test
    void executarLancaExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, USER_ID))
                .isInstanceOf(CentroCustoNaoEncontradoException.class);
        verify(repository, times(0)).save(any(CentroCusto.class));
    }
}
