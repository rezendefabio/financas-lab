package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import com.laboratorio.financas.lembrete.domain.PrioridadeLembrete;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarLembretesUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private LembreteRepository repository;
    private ListarLembretesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LembreteRepository.class);
        useCase = new ListarLembretesUseCase(repository);
    }

    @Test
    void executarRetornaListaDoUsuario() {
        Lembrete l = new Lembrete(USER_ID, "A", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(l));

        List<Lembrete> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitulo()).isEqualTo("A");
    }
}
