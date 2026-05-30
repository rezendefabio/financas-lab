package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

class AtualizarLembreteUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private LembreteRepository repository;
    private AtualizarLembreteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LembreteRepository.class);
        useCase = new AtualizarLembreteUseCase(repository);
    }

    @Test
    void executarAtualizaCamposERetorna() {
        Lembrete l = new Lembrete(USER_ID, "Antigo", "desc",
                LocalDate.of(2026, 6, 1), PrioridadeLembrete.BAIXA, false);
        when(repository.buscarPorId(l.getId())).thenReturn(Optional.of(l));
        when(repository.atualizar(any(Lembrete.class))).thenReturn(l);

        AtualizarLembreteUseCase.Comando cmd = new AtualizarLembreteUseCase.Comando(
                l.getId(), USER_ID, "Novo", "nova desc",
                LocalDate.of(2026, 7, 15), PrioridadeLembrete.ALTA, true);

        Lembrete resultado = useCase.executar(cmd);

        assertThat(resultado.getTitulo()).isEqualTo("Novo");
        assertThat(resultado.getPrioridade()).isEqualTo(PrioridadeLembrete.ALTA);
        assertThat(resultado.isConcluido()).isTrue();
        verify(repository, times(1)).atualizar(any(Lembrete.class));
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarLembreteUseCase.Comando cmd = new AtualizarLembreteUseCase.Comando(
                id, USER_ID, "T", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);

        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(LembreteNaoEncontradoException.class);
        verify(repository, never()).atualizar(any());
    }

    @Test
    void executarComLembreteDeOutroUsuarioLancaExcecao() {
        Lembrete l = new Lembrete(UUID.randomUUID(), "A", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);
        when(repository.buscarPorId(l.getId())).thenReturn(Optional.of(l));

        AtualizarLembreteUseCase.Comando cmd = new AtualizarLembreteUseCase.Comando(
                l.getId(), USER_ID, "T", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);

        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(LembreteNaoEncontradoException.class);
        verify(repository, never()).atualizar(any());
    }
}
