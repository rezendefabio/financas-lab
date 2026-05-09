package com.laboratorio.financas.conta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarContaPorIdUseCaseTest {

    private ContaRepository repository;
    private BuscarContaPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ContaRepository.class);
        useCase = new BuscarContaPorIdUseCase(repository);
    }

    @Test
    void executarRetornaContaQuandoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        Conta conta = new Conta(
                "Corrente",
                TipoConta.CORRENTE,
                new Money(BigDecimal.valueOf(100), Currency.getInstance("BRL"))
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(conta));

        // When
        Conta resultado = useCase.executar(id);

        // Then
        assertThat(resultado).isSameAs(conta);
    }

    @Test
    void executarLancaContaNaoEncontradaExceptionComIdCorretoQuandoNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(ContaNaoEncontradaException.class)
                .satisfies(ex -> {
                    ContaNaoEncontradaException cnee = (ContaNaoEncontradaException) ex;
                    assertThat(cnee.getId()).isEqualTo(id);
                });
    }

    @Test
    void executarChamaRepositorioUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        Conta conta = new Conta(
                "Poupanca",
                TipoConta.POUPANCA,
                new Money(BigDecimal.valueOf(500), Currency.getInstance("BRL"))
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(conta));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
    }
}
