package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarEmprestimosUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private EmprestimoRepository repository;
    private ListarEmprestimosUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new ListarEmprestimosUseCase(repository);
    }

    @Test
    void executarRetornaListaDoRepositorio() {
        Money m = new Money(new BigDecimal("10.00"), Currency.getInstance("BRL"));
        Emprestimo e = new Emprestimo(USER_ID, "X", null, TipoEmprestimo.CONCEDIDO,
                m, LocalDate.of(2026, 1, 1), false);
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(e));

        List<Emprestimo> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(1).containsExactly(e);
    }
}
