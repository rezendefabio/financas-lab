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

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate DATA = LocalDate.of(2026, 5, 30);

    private EmprestimoRepository repository;
    private AtualizarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new AtualizarEmprestimoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizAtualizaEmprestimo() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        Emprestimo existente = new Emprestimo(USER_ID, "Antiga", "Joao",
                TipoEmprestimo.CONCEDIDO, valor, DATA);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        Money novoValor = new Money(new BigDecimal("250.00"), BRL);
        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                existente.getId(), "Nova", "Maria", TipoEmprestimo.RECEBIDO, novoValor, DATA, true);
        Emprestimo resultado = useCase.executar(cmd);

        assertThat(resultado.getDescricao()).isEqualTo("Nova");
        assertThat(resultado.isQuitado()).isTrue();
        verify(repository).atualizar(any(Emprestimo.class));
    }

    @Test
    void executarComIdInexistenteLancaEmprestimoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        Money valor = new Money(new BigDecimal("100.00"), BRL);

        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                id, "Nova", "Maria", TipoEmprestimo.RECEBIDO, valor, DATA, true);
        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);

        verify(repository, never()).atualizar(any());
    }
}
