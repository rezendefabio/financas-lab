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

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Money VALOR = new Money(new BigDecimal("100.00"), Currency.getInstance("BRL"));
    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

    private EmprestimoRepository repository;
    private AtualizarEmprestimoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmprestimoRepository.class);
        useCase = new AtualizarEmprestimoUseCase(repository);
    }

    @Test
    void executarComIdExistenteAtualizaERetorna() {
        Emprestimo existente = new Emprestimo(USER_ID, "Antigo", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Emprestimo.class))).thenReturn(existente);

        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                existente.getId(), "Novo", "Joao",
                TipoEmprestimo.RECEBIDO, VALOR, DATA, true);
        Emprestimo resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        verify(repository).atualizar(any(Emprestimo.class));
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarEmprestimoUseCase.Comando cmd = new AtualizarEmprestimoUseCase.Comando(
                id, "X", null, TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);

        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(EmprestimoNaoEncontradoException.class);
        verify(repository, never()).atualizar(any());
    }
}
