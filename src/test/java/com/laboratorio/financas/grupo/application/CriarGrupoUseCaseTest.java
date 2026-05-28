package com.laboratorio.financas.grupo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarGrupoUseCaseTest {

    private GrupoRepository repository;
    private CriarGrupoUseCase useCase;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(GrupoRepository.class);
        useCase = new CriarGrupoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaGrupoCriado() {
        Grupo grupoSalvo = new Grupo(USER_ID, "Viagem Europa", "Gastos da viagem");
        when(repository.salvar(any(Grupo.class))).thenReturn(grupoSalvo);

        CriarGrupoUseCase.Comando comando =
                new CriarGrupoUseCase.Comando(USER_ID, "Viagem Europa", "Gastos da viagem");
        Grupo resultado = useCase.executar(comando);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Viagem Europa");
    }

    @Test
    void executarChamaRepositorioSalvarUmaVez() {
        Grupo grupoSalvo = new Grupo(USER_ID, "Casa Nova", null);
        when(repository.salvar(any(Grupo.class))).thenReturn(grupoSalvo);

        useCase.executar(new CriarGrupoUseCase.Comando(USER_ID, "Casa Nova", null));

        verify(repository, times(1)).salvar(any(Grupo.class));
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        Grupo grupoSalvo = new Grupo(USER_ID, "Reforma", "Detalhes");
        when(repository.salvar(any(Grupo.class))).thenReturn(grupoSalvo);

        Grupo resultado = useCase.executar(new CriarGrupoUseCase.Comando(USER_ID, "Reforma", "Detalhes"));

        assertThat(resultado).isSameAs(grupoSalvo);
    }
}
