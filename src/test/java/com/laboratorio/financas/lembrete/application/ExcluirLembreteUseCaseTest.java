package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteNaoEncontradoException;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import com.laboratorio.financas.lembrete.domain.PrioridadeLembrete;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExcluirLembreteUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private LembreteRepository repository;
    private ExcluirLembreteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LembreteRepository.class);
        useCase = new ExcluirLembreteUseCase(repository);
    }

    @Test
    void executarComLembreteExistenteRemove() {
        Lembrete l = new Lembrete(USER_ID, "X", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);
        when(repository.buscarPorId(l.getId())).thenReturn(Optional.of(l));

        useCase.executar(l.getId(), USER_ID);

        verify(repository, times(1)).deletar(l.getId());
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, USER_ID))
                .isInstanceOf(LembreteNaoEncontradoException.class);
        verify(repository, never()).deletar(id);
    }

    @Test
    void executarComLembreteDeOutroUsuarioLancaExcecao() {
        Lembrete l = new Lembrete(UUID.randomUUID(), "X", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);
        when(repository.buscarPorId(l.getId())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> useCase.executar(l.getId(), USER_ID))
                .isInstanceOf(LembreteNaoEncontradoException.class);
        verify(repository, never()).deletar(l.getId());
    }
}
