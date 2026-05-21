package com.laboratorio.financas.centrocusto.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.centrocusto.application.dto.AtualizarCentroCustoComando;
import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoNaoEncontradoException;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarCentroCustoUseCaseTest {

    private CentroCustoRepository repository;
    private AtualizarCentroCustoUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CentroCustoRepository.class);
        useCase = new AtualizarCentroCustoUseCase(repository);
    }

    @Test
    void executarAtualizaNomeEDescricao() {
        CentroCusto existente = new CentroCusto(USER_ID, "Casa", "old");
        when(repository.findByIdAndUserId(existente.getId(), USER_ID)).thenReturn(Optional.of(existente));
        when(repository.save(any(CentroCusto.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarCentroCustoComando comando = new AtualizarCentroCustoComando(
                existente.getId(), USER_ID, "Trabalho", "nova desc");

        CentroCusto resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Trabalho");
        assertThat(resultado.getDescricao()).isEqualTo("nova desc");
        verify(repository, times(1)).save(any(CentroCusto.class));
    }

    @Test
    void executarLancaExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(
                new AtualizarCentroCustoComando(id, USER_ID, "Casa", null)))
                .isInstanceOf(CentroCustoNaoEncontradoException.class);
    }

    @Test
    void executarPreservaAtivoEUserIdDoExistente() {
        CentroCusto existente = new CentroCusto(USER_ID, "Casa", null);
        CentroCusto desativado = existente.desativar();
        when(repository.findByIdAndUserId(desativado.getId(), USER_ID)).thenReturn(Optional.of(desativado));
        when(repository.save(any(CentroCusto.class))).thenAnswer(inv -> inv.getArgument(0));

        CentroCusto resultado = useCase.executar(
                new AtualizarCentroCustoComando(desativado.getId(), USER_ID, "Renomeado", null));

        assertThat(resultado.isAtivo()).isFalse();
        assertThat(resultado.getUserId()).isEqualTo(USER_ID);
    }
}
