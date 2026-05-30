package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class ExcluirEmprestimoUseCaseTest {

    private EmprestimoRepository repository;
    private ExcluirEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new ExcluirEmprestimoUseCase(repository);
    }

    @Test
    void executarComIdExistenteChamaDeletar() {
        Emprestimo e = new Emprestimo(UUID.randomUUID(), "X", null,
                TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("100.00"), Currency.getInstance("BRL")),
                LocalDate.of(2026, 1, 15), false);
        when(repository.buscarPorId(e.getId())).thenReturn(Optional.of(e));

        useCase.executar(e.getId());

        verify(repository).deletar(e.getId());
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
