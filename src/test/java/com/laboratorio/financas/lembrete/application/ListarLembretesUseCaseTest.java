package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import com.laboratorio.financas.lembrete.domain.Prioridade;
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
    void executarRetornaTodosOsLembretes() {
        List<Lembrete> esperado = List.of(
                new Lembrete(USER_ID, "A", null, LocalDate.now(), Prioridade.BAIXA));
        when(repository.listarTodos()).thenReturn(esperado);

        List<Lembrete> resultado = useCase.executar();

        assertThat(resultado).isEqualTo(esperado);
    }
}
