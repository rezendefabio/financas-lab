package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

class DeletarEmprestimoUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private EmprestimoRepository repository;
    private DeletarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new DeletarEmprestimoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizDeletaEmprestimo() {
        Emprestimo existente = new Emprestimo(USER_ID, "Desc", "Joao",
                TipoEmprestimo.CONCEDIDO, new Money(new BigDecimal("100.00"), BRL),
                LocalDate.of(2026, 1, 1));
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        useCase.executar(existente.getId());

        verify(repository, times(1)).deletar(existente.getId());
    }

    @Test
    void executarComIdInexistenteLancaEmprestimoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);

        verify(repository, never()).deletar(id);
    }
}
