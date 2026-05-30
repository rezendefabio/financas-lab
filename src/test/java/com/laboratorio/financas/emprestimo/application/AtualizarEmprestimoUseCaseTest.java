package com.laboratorio.financas.emprestimo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

class AtualizarEmprestimoUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private EmprestimoRepository repository;
    private AtualizarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new AtualizarEmprestimoUseCase(repository);
    }

    @Test
    void executarComIdExistenteAtualizaCampos() {
        Money valor = new Money(new BigDecimal("10.00"), Currency.getInstance("BRL"));
        Emprestimo existente = new Emprestimo(USER_ID, "Antigo", null,
                TipoEmprestimo.CONCEDIDO, valor, LocalDate.now(), false);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Emprestimo.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                existente.getId(), "Novo", "Maria", TipoEmprestimo.RECEBIDO,
                new BigDecimal("20.00"), "BRL", LocalDate.now(), true);

        Emprestimo resultado = useCase.executar(cmd);

        assertThat(resultado.getDescricao()).isEqualTo("Novo");
        assertThat(resultado.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(resultado.isQuitado()).isTrue();
        verify(repository).atualizar(any(Emprestimo.class));
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                id, "X", null, TipoEmprestimo.CONCEDIDO,
                new BigDecimal("10.00"), "BRL", LocalDate.now(), false);

        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);

        verify(repository, never()).atualizar(any());
    }
}
