package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class BuscarLembretePorIdUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private LembreteRepository repository;
    private BuscarLembretePorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LembreteRepository.class);
        useCase = new BuscarLembretePorIdUseCase(repository);
    }

    @Test
    void executarComLembreteExistenteEDoUsuarioRetornaLembrete() {
        Lembrete l = new Lembrete(USER_ID, "Titulo", null,
                LocalDate.now(), PrioridadeLembrete.MEDIA, false);
        when(repository.buscarPorId(l.getId())).thenReturn(Optional.of(l));

        Lembrete resultado = useCase.executar(l.getId(), USER_ID);

        assertThat(resultado).isEqualTo(l);
    }

    @Test
    void executarComIdInexistenteLancaLembreteNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, USER_ID))
                .isInstanceOf(LembreteNaoEncontradoException.class);
    }

    @Test
    void executarComLembreteDeOutroUsuarioLancaLembreteNaoEncontrado() {
        Lembrete l = new Lembrete(UUID.randomUUID(), "Titulo", null,
                LocalDate.now(), PrioridadeLembrete.MEDIA, false);
        when(repository.buscarPorId(l.getId())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> useCase.executar(l.getId(), USER_ID))
                .isInstanceOf(LembreteNaoEncontradoException.class);
    }
}
