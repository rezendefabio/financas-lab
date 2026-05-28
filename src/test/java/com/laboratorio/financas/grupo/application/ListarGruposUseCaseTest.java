package com.laboratorio.financas.grupo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarGruposUseCaseTest {

    private GrupoRepository repository;
    private ListarGruposUseCase useCase;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(GrupoRepository.class);
        useCase = new ListarGruposUseCase(repository);
    }

    @Test
    void executarRetornaListaDeGrupos() {
        Grupo g1 = new Grupo(USER_ID, "Viagem", null);
        Grupo g2 = new Grupo(USER_ID, "Casa", "Desc");
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(g1, g2));

        List<Grupo> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).contains(g1, g2);
    }

    @Test
    void executarRetornaListaVaziaQuandoNaoHaGrupos() {
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of());

        List<Grupo> resultado = useCase.executar(USER_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void executarPassaUserIdCorretoParaRepositorio() {
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of());

        useCase.executar(USER_ID);

        Mockito.verify(repository).listarPorUserId(USER_ID);
    }
}
