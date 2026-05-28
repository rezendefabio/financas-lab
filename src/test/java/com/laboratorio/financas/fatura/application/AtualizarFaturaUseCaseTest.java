package com.laboratorio.financas.fatura.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaNaoEncontradaException;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarFaturaUseCaseTest {

    private FaturaRepository repository;
    private AtualizarFaturaUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTA_ID = UUID.randomUUID();
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 6, 10);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FaturaRepository.class);
        useCase = new AtualizarFaturaUseCase(repository);
    }

    @Test
    void executarAtualizaCamposEPersiste() {
        Fatura existente = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate novoVencimento = LocalDate.of(2026, 7, 10);
        AtualizarFaturaUseCase.Comando comando = new AtualizarFaturaUseCase.Comando(
                existente.getId(), "Cartao Julho", novoVencimento, null,
                new BigDecimal("2000.00"), "BRL");
        Fatura resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Cartao Julho");
        assertThat(resultado.getDataVencimento()).isEqualTo(novoVencimento);
        assertThat(resultado.getValorTotal().valor()).isEqualByComparingTo("2000.00");
        verify(repository, times(1)).atualizar(any(Fatura.class));
    }

    @Test
    void executarLancaExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarFaturaUseCase.Comando comando = new AtualizarFaturaUseCase.Comando(
                id, "Cartao", VENCIMENTO, null, null, null);

        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(FaturaNaoEncontradaException.class);
        verify(repository, never()).atualizar(any(Fatura.class));
    }
}
