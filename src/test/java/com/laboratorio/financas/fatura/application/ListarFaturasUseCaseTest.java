package com.laboratorio.financas.fatura.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarFaturasUseCaseTest {

    private FaturaRepository repository;
    private ListarFaturasUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTA_ID = UUID.randomUUID();
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 6, 10);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FaturaRepository.class);
        useCase = new ListarFaturasUseCase(repository);
    }

    @Test
    void executarRetornaFaturasDoRepositorio() {
        Fatura f1 = new Fatura(USER_ID, CONTA_ID, "Maio", VENCIMENTO, null, null);
        Fatura f2 = new Fatura(USER_ID, CONTA_ID, "Junho", VENCIMENTO, null, null);
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(f1, f2));

        List<Fatura> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void executarRetornaListaVaziaQuandoSemRegistros() {
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of());

        List<Fatura> resultado = useCase.executar(USER_ID);

        assertThat(resultado).isEmpty();
    }
}
