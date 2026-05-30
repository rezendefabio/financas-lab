package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoNaoEncontradoException;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarEmprestimoUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Currency BRL = Currency.getInstance("BRL");

    private EmprestimoRepository repository;
    private AtualizarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new AtualizarEmprestimoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizAtualizaEmprestimo() {
        UUID id = UUID.randomUUID();
        Emprestimo existente = new Emprestimo(USER_ID, "antigo", null,
                TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("100.00"), BRL),
                LocalDate.now());
        when(repository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        Money novo = new Money(new BigDecimal("200.00"), BRL);
        Emprestimo resultado = useCase.executar(new AtualizarEmprestimoUseCase.Comando(
                id, "novo", "Maria", TipoEmprestimo.RECEBIDO, novo, LocalDate.now(), true));

        assertThat(resultado.getDescricao()).isEqualTo("novo");
        assertThat(resultado.isQuitado()).isTrue();
        verify(repository).atualizar(any(Emprestimo.class));
    }

    @Test
    void executarComIdInexistenteLancaEmprestimoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(new AtualizarEmprestimoUseCase.Comando(
                id, "x", null, TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("1.00"), BRL), LocalDate.now(), false)))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);
        verify(repository, never()).atualizar(any());
    }

    @Test
    void executarComValorZeroLancaIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        Money zero = new Money(BigDecimal.ZERO, BRL);

        assertThatThrownBy(() -> useCase.executar(new AtualizarEmprestimoUseCase.Comando(
                id, "x", null, TipoEmprestimo.CONCEDIDO, zero, LocalDate.now(), false)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).atualizar(any());
    }
}
