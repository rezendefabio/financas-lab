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

    private EmprestimoRepository repository;
    private DeletarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new DeletarEmprestimoUseCase(repository);
    }

    @Test
    void executarComIdExistenteDeleta() {
        UUID id = UUID.randomUUID();
        Money m = new Money(new BigDecimal("10.00"), Currency.getInstance("BRL"));
        Emprestimo e = new Emprestimo(UUID.randomUUID(), "X", null, TipoEmprestimo.CONCEDIDO,
                m, LocalDate.of(2026, 1, 1), false);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(e));

        useCase.executar(id);

        verify(repository, times(1)).deletar(id);
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);
        verify(repository, never()).deletar(id);
    }
}
