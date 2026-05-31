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

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

    private EmprestimoRepository repository;
    private BuscarEmprestimoPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new BuscarEmprestimoPorIdUseCase(repository);
    }

    @Test
    void executarComIdExistenteRetornaEmprestimo() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        Emprestimo e = new Emprestimo(USER_ID, "A", null,
                TipoEmprestimo.CONCEDIDO, valor, DATA, false);
        when(repository.buscarPorId(e.getId())).thenReturn(Optional.of(e));

        Emprestimo resultado = useCase.executar(e.getId());

        assertThat(resultado.getId()).isEqualTo(e.getId());
    }

    @Test
    void executarComIdInexistenteLancaEmprestimoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);
    }
}
