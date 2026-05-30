package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class BuscarLembreteUseCaseTest {

    private LembreteRepository repository;
    private BuscarLembreteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LembreteRepository.class);
        useCase = new BuscarLembreteUseCase(repository);
    }

    @Test
    void executarComIdExistenteRetornaLembrete() {
        Lembrete l = new Lembrete(UUID.randomUUID(), "T", null, LocalDate.now(), Prioridade.BAIXA);
        when(repository.buscarPorId(l.getId())).thenReturn(Optional.of(l));

        assertThat(useCase.executar(l.getId())).isEqualTo(l);
    }

    @Test
    void executarComIdInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(LembreteNaoEncontradoException.class);
    }
}
