package com.laboratorio.financas.limite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteNaoEncontradoException;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarLimiteUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private LimiteRepository repository;
    private AtualizarLimiteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LimiteRepository.class);
        useCase = new AtualizarLimiteUseCase(repository);
    }

    private Money valor(String v) {
        return new Money(new BigDecimal(v), BRL);
    }

    @Test
    void executarCaminhoFelizAtualizaLimite() {
        Limite existente = new Limite(USER_ID, "Antigo", TipoLimite.DIARIO, valor("50.00"));
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Limite.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarLimiteUseCase.Comando cmd = new AtualizarLimiteUseCase.Comando(
                existente.getId(), USER_ID, "Novo", TipoLimite.SEMANAL, valor("80.00"));
        Limite resultado = useCase.executar(cmd);

        assertThat(resultado.getNome()).isEqualTo("Novo");
        assertThat(resultado.getTipo()).isEqualTo(TipoLimite.SEMANAL);
        verify(repository).atualizar(any(Limite.class));
    }

    @Test
    void executarComIdInexistenteLancaLimiteNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarLimiteUseCase.Comando cmd = new AtualizarLimiteUseCase.Comando(
                id, USER_ID, "X", TipoLimite.DIARIO, valor("10.00"));
        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(LimiteNaoEncontradoException.class);

        verify(repository, never()).atualizar(any());
    }

    @Test
    void executarComLimiteDeOutroUsuarioLancaLimiteNaoEncontradoException() {
        Limite outro = new Limite(UUID.randomUUID(), "Antigo", TipoLimite.DIARIO, valor("50.00"));
        when(repository.buscarPorId(outro.getId())).thenReturn(Optional.of(outro));

        AtualizarLimiteUseCase.Comando cmd = new AtualizarLimiteUseCase.Comando(
                outro.getId(), USER_ID, "X", TipoLimite.DIARIO, valor("10.00"));
        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(LimiteNaoEncontradoException.class);

        verify(repository, never()).atualizar(any());
    }
}
