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
    private static final LocalDate DATA = LocalDate.of(2026, 5, 30);

    private EmprestimoRepository repository;
    private BuscarEmprestimoPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new BuscarEmprestimoPorIdUseCase(repository);
    }

    @Test
    void executarComIdExistenteRetornaEmprestimo() {
        Emprestimo existente = new Emprestimo(USER_ID, "Teste", "T",
                TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")), DATA);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        Emprestimo resultado = useCase.executar(existente.getId());

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDescricao()).isEqualTo("Teste");
    }

    @Test
    void executarComIdInexistenteLancaEmprestimoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);
    }
}
