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
    void executarComIdExistenteAtualizaERetornaEmprestimo() {
        UUID id = UUID.randomUUID();
        Money valorAntigo = new Money(new BigDecimal("100.00"), BRL);
        Emprestimo existente = new Emprestimo(USER_ID, "Antiga", null,
                TipoEmprestimo.CONCEDIDO, valorAntigo, LocalDate.of(2026, 1, 1), false);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                id, "Nova", "Maria", TipoEmprestimo.RECEBIDO,
                new BigDecimal("200.00"), "BRL", LocalDate.of(2026, 2, 1), true);

        Emprestimo r = useCase.executar(cmd);

        assertThat(r.getDescricao()).isEqualTo("Nova");
        assertThat(r.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(r.isQuitado()).isTrue();
        verify(repository).atualizar(any(Emprestimo.class));
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                id, "X", null, TipoEmprestimo.CONCEDIDO,
                new BigDecimal("1.00"), "BRL", LocalDate.of(2026, 1, 1), false);

        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);
        verify(repository, never()).atualizar(any());
    }
}
