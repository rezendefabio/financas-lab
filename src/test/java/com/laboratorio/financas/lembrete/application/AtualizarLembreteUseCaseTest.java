package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteNaoEncontradoException;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import com.laboratorio.financas.lembrete.domain.Prioridade;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarLembreteUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATA = LocalDate.of(2026, 6, 1);

    private LembreteRepository repository;
    private AtualizarLembreteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LembreteRepository.class);
        useCase = new AtualizarLembreteUseCase(repository);
    }

    @Test
    void executarAtualizaERetornaSalvo() {
        Lembrete existente = new Lembrete(USER_ID, "Antigo", null, DATA, Prioridade.BAIXA);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Lembrete.class))).thenAnswer(i -> i.getArgument(0));

        AtualizarLembreteUseCase.Comando cmd = new AtualizarLembreteUseCase.Comando(
                existente.getId(), "Novo", "Desc", DATA, Prioridade.ALTA, true);
        Lembrete resultado = useCase.executar(cmd);

        assertThat(resultado.getTitulo()).isEqualTo("Novo");
        assertThat(resultado.getPrioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(resultado.isConcluido()).isTrue();
        verify(repository).atualizar(any(Lembrete.class));
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarLembreteUseCase.Comando cmd = new AtualizarLembreteUseCase.Comando(
                id, "T", null, DATA, Prioridade.BAIXA, false);
        assertThatThrownBy(() -> useCase.executar(cmd))
                .isInstanceOf(LembreteNaoEncontradoException.class);
        verify(repository, never()).atualizar(any());
    }
}
