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
    private static final Currency BRL = Currency.getInstance("BRL");

    private EmprestimoRepository repository;
    private ListarEmprestimosUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new ListarEmprestimosUseCase(repository);
    }

    @Test
    void executarRetornaListaDoUsuario() {
        Emprestimo e = new Emprestimo(USER_ID, "x", null,
                TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("1.00"), BRL),
                LocalDate.now());
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(e));

        List<Emprestimo> lista = useCase.executar(USER_ID);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getUserId()).isEqualTo(USER_ID);
    }
}
