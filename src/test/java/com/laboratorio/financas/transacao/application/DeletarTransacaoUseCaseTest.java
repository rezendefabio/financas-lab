package com.laboratorio.financas.transacao.application;

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

class DeletarTransacaoUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private TransacaoRepository repository;
    private DeletarTransacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TransacaoRepository.class);
        useCase = new DeletarTransacaoUseCase(repository);
    }

    private Transacao transacaoValida(UUID contaId) {
        return new Transacao(
                TipoTransacao.RECEITA,
                new Money(BigDecimal.valueOf(100), BRL),
                LocalDate.of(2025, 1, 15),
                "Salario",
                contaId,
                null,
                null
        );
    }

    @Test
    void executarCaminhoFelizChamaRepositorioDeletar() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(transacaoValida(contaId)));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).deletar(id);
    }

    @Test
    void executarLancaTransacaoNaoEncontradaExceptionQuandoIdNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(TransacaoNaoEncontradaException.class);
    }

    @Test
    void executarNaoDeletaQuandoNaoEncontrada() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        // When
        try {
            useCase.executar(id);
        } catch (TransacaoNaoEncontradaException e) {
            // esperado
        }

        // Then
        verify(repository, times(0)).deletar(id);
    }

    @Test
    void executarChamaRepositorioBuscarPorIdUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(transacaoValida(contaId)));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).buscarPorId(id);
    }
}
