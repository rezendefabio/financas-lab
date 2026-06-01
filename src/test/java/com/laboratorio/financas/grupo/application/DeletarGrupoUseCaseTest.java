package com.laboratorio.financas.grupo.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.grupo.domain.GrupoNaoEncontradoException;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeletarGrupoUseCaseTest {

    private GrupoRepository repository;
    private DeletarGrupoUseCase useCase;

    private static final UUID GRUPO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(GrupoRepository.class);
        useCase = new DeletarGrupoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizChamaDeletar() {
        Grupo grupo = new Grupo(GRUPO_ID, USER_ID, "Viagem", null, true, Instant.now(), Instant.now());
        when(repository.buscarPorId(GRUPO_ID)).thenReturn(Optional.of(grupo));

        useCase.executar(GRUPO_ID);

        verify(repository, times(1)).deletar(GRUPO_ID);
    }

    @Test
    void executarGrupoNaoEncontradoLancaException() {
        when(repository.buscarPorId(GRUPO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(GRUPO_ID))
                .isInstanceOf(GrupoNaoEncontradoException.class);

        verify(repository, never()).deletar(GRUPO_ID);
    }

    @Test
    void executarRemoveGrupoCriadoPorOutroUsuario() {
        UUID outroUserId = UUID.randomUUID();
        Grupo grupo = new Grupo(GRUPO_ID, outroUserId, "Viagem", null, true, Instant.now(), Instant.now());
        when(repository.buscarPorId(GRUPO_ID)).thenReturn(Optional.of(grupo));

        useCase.executar(GRUPO_ID);

        verify(repository, times(1)).deletar(GRUPO_ID);
    }
}
