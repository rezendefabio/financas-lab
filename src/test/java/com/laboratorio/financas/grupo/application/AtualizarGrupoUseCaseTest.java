package com.laboratorio.financas.grupo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

class AtualizarGrupoUseCaseTest {

    private GrupoRepository repository;
    private AtualizarGrupoUseCase useCase;

    private static final UUID GRUPO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(GrupoRepository.class);
        useCase = new AtualizarGrupoUseCase(repository);
    }

    @Test
    void executarAtualizaNomeEDescricao() {
        Grupo existente = new Grupo(GRUPO_ID, USER_ID, "Velho", "Desc velha", true, Instant.now(), Instant.now());
        when(repository.buscarPorId(GRUPO_ID)).thenReturn(Optional.of(existente));
        when(repository.salvar(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarGrupoUseCase.Comando comando =
                new AtualizarGrupoUseCase.Comando(GRUPO_ID, "Novo", "Desc nova");
        Grupo resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Novo");
        assertThat(resultado.getDescricao()).isEqualTo("Desc nova");
        assertThat(resultado.getId()).isEqualTo(GRUPO_ID);
    }

    @Test
    void executarGrupoNaoEncontradoLancaException() {
        when(repository.buscarPorId(GRUPO_ID)).thenReturn(Optional.empty());

        AtualizarGrupoUseCase.Comando comando =
                new AtualizarGrupoUseCase.Comando(GRUPO_ID, "Novo", null);

        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(GrupoNaoEncontradoException.class);

        verify(repository, never()).salvar(any(Grupo.class));
    }

    @Test
    void executarAtualizaGrupoCriadoPorOutroUsuario() {
        UUID outroUserId = UUID.randomUUID();
        Grupo existente = new Grupo(GRUPO_ID, outroUserId, "Velho", "Desc velha", true, Instant.now(), Instant.now());
        when(repository.buscarPorId(GRUPO_ID)).thenReturn(Optional.of(existente));
        when(repository.salvar(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarGrupoUseCase.Comando comando =
                new AtualizarGrupoUseCase.Comando(GRUPO_ID, "Novo", "Desc nova");
        Grupo resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Novo");
        assertThat(resultado.getUserId()).isEqualTo(outroUserId);
        verify(repository).salvar(any(Grupo.class));
    }
}
