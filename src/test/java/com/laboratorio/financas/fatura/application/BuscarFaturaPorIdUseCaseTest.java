package com.laboratorio.financas.fatura.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaNaoEncontradaException;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarFaturaPorIdUseCaseTest {

    private FaturaRepository repository;
    private BuscarFaturaPorIdUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTA_ID = UUID.randomUUID();
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 6, 10);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FaturaRepository.class);
        useCase = new BuscarFaturaPorIdUseCase(repository);
    }

    @Test
    void executarRetornaFaturaQuandoExiste() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        when(repository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));

        Fatura resultado = useCase.executar(fatura.getId());

        assertThat(resultado).isEqualTo(fatura);
    }

    @Test
    void executarLancaExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(FaturaNaoEncontradaException.class)
                .hasMessageContaining(id.toString());
    }
}
