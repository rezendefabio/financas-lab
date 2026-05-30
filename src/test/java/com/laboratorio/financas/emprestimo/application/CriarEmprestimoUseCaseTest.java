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

    private EmprestimoRepository repository;
    private CriarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new CriarEmprestimoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizSalvaERetornaEntidade() {
        Money valor = new Money(new BigDecimal("150.00"), Currency.getInstance("BRL"));
        Emprestimo salvo = new Emprestimo(
                USER_ID, "Teste", "Joao", TipoEmprestimo.CONCEDIDO,
                valor, LocalDate.now(), false);
        when(repository.salvar(any(Emprestimo.class))).thenReturn(salvo);

        CriarEmprestimoUseCase.Comando cmd = new CriarEmprestimoUseCase.Comando(
                USER_ID, "Teste", "Joao", TipoEmprestimo.CONCEDIDO,
                new BigDecimal("150.00"), "BRL", LocalDate.now(), false);
        Emprestimo resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDescricao()).isEqualTo("Teste");
        verify(repository, times(1)).salvar(any(Emprestimo.class));
    }
}
