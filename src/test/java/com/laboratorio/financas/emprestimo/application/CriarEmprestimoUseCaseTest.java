package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

    private EmprestimoRepository repository;
    private CriarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new CriarEmprestimoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaEmprestimoCriado() {
        Emprestimo salvo = new Emprestimo(USER_ID, "Emprestimo", "Joao",
                TipoEmprestimo.CONCEDIDO, new Money(new BigDecimal("100.00"), BRL), DATA);
        when(repository.salvar(any(Emprestimo.class))).thenReturn(salvo);

        CriarEmprestimoUseCase.Comando cmd = new CriarEmprestimoUseCase.Comando(
                USER_ID, "Emprestimo", "Joao", TipoEmprestimo.CONCEDIDO,
                new BigDecimal("100.00"), "BRL", DATA, false);
        Emprestimo resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDescricao()).isEqualTo("Emprestimo");
        verify(repository, times(1)).salvar(any(Emprestimo.class));
    }

    @Test
    void executarComQuitadoTrueMarcaEmprestimoComoQuitado() {
        when(repository.salvar(any(Emprestimo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CriarEmprestimoUseCase.Comando cmd = new CriarEmprestimoUseCase.Comando(
                USER_ID, "Emprestimo", "Joao", TipoEmprestimo.RECEBIDO,
                new BigDecimal("50.00"), "BRL", DATA, true);
        Emprestimo resultado = useCase.executar(cmd);

        assertThat(resultado.isQuitado()).isTrue();
    }
}
