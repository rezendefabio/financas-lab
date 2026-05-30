package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarEmprestimoUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Currency BRL = Currency.getInstance("BRL");

    private EmprestimoRepository repository;
    private CriarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new CriarEmprestimoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaEmprestimoCriado() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        Emprestimo salvo = new Emprestimo(USER_ID, "desc", "Joao",
                TipoEmprestimo.CONCEDIDO, valor, LocalDate.now());
        when(repository.salvar(any(Emprestimo.class))).thenReturn(salvo);

        Emprestimo resultado = useCase.executar(new CriarEmprestimoUseCase.Comando(
                USER_ID, "desc", "Joao", TipoEmprestimo.CONCEDIDO, valor, LocalDate.now()));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDescricao()).isEqualTo("desc");
        verify(repository).salvar(any(Emprestimo.class));
    }

    @Test
    void executarComValorZeroLancaIllegalArgumentException() {
        Money zero = new Money(BigDecimal.ZERO, BRL);
        assertThatIllegalArgumentException().isThrownBy(() -> useCase.executar(
                new CriarEmprestimoUseCase.Comando(USER_ID, "x", null,
                        TipoEmprestimo.CONCEDIDO, zero, LocalDate.now())))
                .withMessageContaining("valor");
        verify(repository, never()).salvar(any());
    }
}
