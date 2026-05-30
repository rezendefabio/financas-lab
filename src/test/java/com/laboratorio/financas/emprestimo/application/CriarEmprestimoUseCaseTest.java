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

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Money VALOR = new Money(new BigDecimal("100.00"), Currency.getInstance("BRL"));
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
        Emprestimo salvo = new Emprestimo(USER_ID, "X", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        when(repository.salvar(any(Emprestimo.class))).thenReturn(salvo);

        CriarEmprestimoUseCase.Comando cmd = new CriarEmprestimoUseCase.Comando(
                USER_ID, "X", null, TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        Emprestimo resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDescricao()).isEqualTo("X");
        verify(repository, times(1)).salvar(any(Emprestimo.class));
    }
}
