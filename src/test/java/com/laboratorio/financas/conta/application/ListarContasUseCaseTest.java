package com.laboratorio.financas.conta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarContasUseCaseTest {

    private ContaRepository repository;
    private ListarContasUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ContaRepository.class);
        useCase = new ListarContasUseCase(repository);
    }

    @Test
    void executarApenasAtivasTrueDelegaParaListarAtivas() {
        // Given
        when(repository.listarAtivas()).thenReturn(List.of());

        // When
        useCase.executar(true);

        // Then
        verify(repository).listarAtivas();
    }

    @Test
    void executarApenasAtivasFalseDelegaParaListarTodas() {
        // Given
        when(repository.listarTodas()).thenReturn(List.of());

        // When
        useCase.executar(false);

        // Then
        verify(repository).listarTodas();
    }

    @Test
    void executarRetornaListaVazia() {
        // Given
        when(repository.listarTodas()).thenReturn(List.of());

        // When
        List<Conta> resultado = useCase.executar(false);

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void executarRetornaListaComMultiplasContas() {
        // Given
        Conta c1 = new Conta(
                "Corrente",
                TipoConta.CORRENTE,
                new Money(BigDecimal.valueOf(100), Currency.getInstance("BRL"))
        );
        Conta c2 = new Conta(
                "Poupanca",
                TipoConta.POUPANCA,
                new Money(BigDecimal.valueOf(200), Currency.getInstance("BRL"))
        );
        when(repository.listarTodas()).thenReturn(List.of(c1, c2));

        // When
        List<Conta> resultado = useCase.executar(false);

        // Then
        assertThat(resultado).hasSize(2);
    }
}
