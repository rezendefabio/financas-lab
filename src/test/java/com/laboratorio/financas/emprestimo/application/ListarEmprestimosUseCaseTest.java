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

    private EmprestimoRepository repository;
    private ListarEmprestimosUseCase useCase;

    private Emprestimo make(String desc) {
        Money valor = new Money(new BigDecimal("10.00"), Currency.getInstance("BRL"));
        return new Emprestimo(USER_ID, desc, null, TipoEmprestimo.CONCEDIDO,
                valor, LocalDate.now(), false);
    }

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new ListarEmprestimosUseCase(repository);
    }

    @Test
    void executarRetornaListaDoRepository() {
        when(repository.listarPorUserId(USER_ID))
                .thenReturn(List.of(make("A"), make("B")));

        List<Emprestimo> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(n -> n.getUserId().equals(USER_ID));
    }
}
