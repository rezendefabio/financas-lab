package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class BuscarEmprestimoPorIdUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private EmprestimoRepository repository;
    private BuscarEmprestimoPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new BuscarEmprestimoPorIdUseCase(repository);
    }

    @Test
    void executarRetornaEmprestimoQuandoExiste() {
        UUID id = UUID.randomUUID();
        Emprestimo e = new Emprestimo(UUID.randomUUID(), "x", null,
                TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("1.00"), BRL),
                LocalDate.now());
        when(repository.buscarPorId(id)).thenReturn(Optional.of(e));

        Emprestimo resultado = useCase.executar(id);

        assertThat(resultado).isSameAs(e);
    }

    @Test
    void executarComIdInexistenteLancaEmprestimoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);
    }
}
