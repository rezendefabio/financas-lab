package com.laboratorio.financas.conta.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

class DesativarContaUseCaseTest {

    private ContaRepository repository;
    private DesativarContaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ContaRepository.class);
        useCase = new DesativarContaUseCase(repository);
    }

    @Test
    void executarCaminhoFelizBuscaDesativaESalva() {
        // Given
        UUID id = UUID.randomUUID();
        Conta conta = new Conta(
                "Corrente",
                TipoConta.CORRENTE,
                new Money(BigDecimal.valueOf(100), Currency.getInstance("BRL"))
        );
        Conta desativada = conta.desativar();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(conta));
        when(repository.salvar(any(Conta.class))).thenReturn(desativada);

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
        verify(repository, times(1)).salvar(any(Conta.class));
    }

    @Test
    void executarLancaContaNaoEncontradaExceptionQuandoIdNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void executarNaoSalvaQuandoContaNaoEncontrada() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When
        try {
            useCase.executar(id);
        } catch (ContaNaoEncontradaException e) {
            // esperado
        }

        // Then
        verify(repository, times(0)).salvar(any(Conta.class));
    }

    @Test
    void executarContaJaInativaChamaRepositorioSalvarUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        Conta contaAtiva = new Conta(
                "Poupanca",
                TipoConta.POUPANCA,
                new Money(BigDecimal.valueOf(200), Currency.getInstance("BRL"))
        );
        Conta contaInativa = contaAtiva.desativar();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(contaInativa));
        when(repository.salvar(any(Conta.class))).thenReturn(contaInativa);

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).salvar(any(Conta.class));
    }

    @Test
    void executarChamaRepositorioBuscarPorIdUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        Conta conta = new Conta(
                "Investimento",
                TipoConta.INVESTIMENTO,
                new Money(BigDecimal.valueOf(1000), Currency.getInstance("USD"))
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(conta));
        when(repository.salvar(any(Conta.class))).thenReturn(conta);

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
    }
}
