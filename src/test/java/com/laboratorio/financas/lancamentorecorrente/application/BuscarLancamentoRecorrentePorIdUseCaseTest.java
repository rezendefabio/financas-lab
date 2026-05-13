package com.laboratorio.financas.lancamentorecorrente.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteNaoEncontradoException;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarLancamentoRecorrentePorIdUseCaseTest {

    private LancamentoRecorrenteRepository repository;
    private BuscarLancamentoRecorrentePorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LancamentoRecorrenteRepository.class);
        useCase = new BuscarLancamentoRecorrentePorIdUseCase(repository);
    }

    @Test
    @DisplayName("executar: retorna lancamento quando encontrado")
    void executarRetornaLancamento() {
        UUID id = UUID.randomUUID();
        LancamentoRecorrente l = new LancamentoRecorrente(
                "Plano", TipoTransacao.DESPESA,
                new Money(new BigDecimal("50.00"), Currency.getInstance("BRL")),
                UUID.randomUUID(), null, Periodicidade.MENSAL, LocalDate.now());
        when(repository.buscarPorId(id)).thenReturn(Optional.of(l));

        LancamentoRecorrente resultado = useCase.executar(id);

        assertThat(resultado).isSameAs(l);
    }

    @Test
    @DisplayName("executar: lanca LancamentoRecorrenteNaoEncontradoException quando ausente")
    void executarLancaExceptionQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(LancamentoRecorrenteNaoEncontradoException.class);
    }
}
