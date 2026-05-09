package com.laboratorio.financas.transacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoNaoEncontradaException;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarTransacaoPorIdUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private TransacaoRepository repository;
    private BuscarTransacaoPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TransacaoRepository.class);
        useCase = new BuscarTransacaoPorIdUseCase(repository);
    }

    @Test
    void executarRetornaTransacaoQuandoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        Transacao transacao = new Transacao(
                TipoTransacao.RECEITA,
                new Money(BigDecimal.valueOf(100), BRL),
                LocalDate.of(2025, 1, 15),
                "Salario",
                UUID.randomUUID(),
                null,
                null
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(transacao));

        // When
        Transacao resultado = useCase.executar(id);

        // Then
        assertThat(resultado).isSameAs(transacao);
    }

    @Test
    void executarLancaTransacaoNaoEncontradaExceptionComIdCorretoQuandoNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(TransacaoNaoEncontradaException.class)
                .satisfies(ex -> {
                    TransacaoNaoEncontradaException tnee = (TransacaoNaoEncontradaException) ex;
                    assertThat(tnee.getId()).isEqualTo(id);
                });
    }

    @Test
    void executarChamaRepositorioUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        Transacao transacao = new Transacao(
                TipoTransacao.DESPESA,
                new Money(BigDecimal.valueOf(50), BRL),
                LocalDate.of(2025, 2, 1),
                "Mercado",
                UUID.randomUUID(),
                null,
                null
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(transacao));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
    }
}
