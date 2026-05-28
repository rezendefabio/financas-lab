package com.laboratorio.financas.fatura.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

class DeletarFaturaUseCaseTest {

    private FaturaRepository repository;
    private DeletarFaturaUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTA_ID = UUID.randomUUID();
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 6, 10);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FaturaRepository.class);
        useCase = new DeletarFaturaUseCase(repository);
    }

    @Test
    void executarDeletaQuandoExiste() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        when(repository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));

        useCase.executar(fatura.getId());

        verify(repository, times(1)).deletar(fatura.getId());
    }

    @Test
    void executarLancaExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(FaturaNaoEncontradaException.class);
        verify(repository, never()).deletar(id);
    }
}
