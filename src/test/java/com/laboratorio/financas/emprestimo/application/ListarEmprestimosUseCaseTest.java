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

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private EmprestimoRepository repository;
    private ListarEmprestimosUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new ListarEmprestimosUseCase(repository);
    }

    @Test
    void executarRetornaTodosOsEmprestimos() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", "Joao",
                TipoEmprestimo.CONCEDIDO, new Money(new BigDecimal("100.00"), BRL),
                LocalDate.of(2026, 1, 1));
        when(repository.listarTodos()).thenReturn(List.of(e));

        List<Emprestimo> resultado = useCase.executar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDescricao()).isEqualTo("Desc");
    }
}
