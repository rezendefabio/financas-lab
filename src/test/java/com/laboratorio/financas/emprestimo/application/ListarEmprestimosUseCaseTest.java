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
    private static final LocalDate DATA = LocalDate.of(2026, 5, 30);

    private EmprestimoRepository repository;
    private ListarEmprestimosUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new ListarEmprestimosUseCase(repository);
    }

    private static Emprestimo emprestimo(String desc) {
        return new Emprestimo(USER_ID, desc, "T", TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")), DATA);
    }

    @Test
    void executarRetornaListaDoRepository() {
        when(repository.listarPorUserId(USER_ID))
                .thenReturn(List.of(emprestimo("A"), emprestimo("B")));

        List<Emprestimo> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(e -> e.getUserId().equals(USER_ID));
    }
}
