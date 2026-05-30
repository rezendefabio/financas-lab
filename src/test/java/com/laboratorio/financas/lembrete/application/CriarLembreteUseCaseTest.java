package com.laboratorio.financas.lembrete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import com.laboratorio.financas.lembrete.domain.Prioridade;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarLembreteUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATA = LocalDate.of(2026, 6, 1);

    private LembreteRepository repository;
    private CriarLembreteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LembreteRepository.class);
        useCase = new CriarLembreteUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaLembreteCriado() {
        Lembrete salvo = new Lembrete(USER_ID, "Teste", null, DATA, Prioridade.ALTA);
        when(repository.salvar(any(Lembrete.class))).thenReturn(salvo);

        CriarLembreteUseCase.Comando cmd = new CriarLembreteUseCase.Comando(
                USER_ID, "Teste", null, DATA, Prioridade.ALTA);
        Lembrete resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTitulo()).isEqualTo("Teste");
        verify(repository, times(1)).salvar(any(Lembrete.class));
    }
}
