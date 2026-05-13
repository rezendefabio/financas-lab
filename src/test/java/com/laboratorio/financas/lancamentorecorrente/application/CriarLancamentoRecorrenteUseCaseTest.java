package com.laboratorio.financas.lancamentorecorrente.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarLancamentoRecorrenteUseCaseTest {

    private LancamentoRecorrenteRepository repository;
    private CriarLancamentoRecorrenteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LancamentoRecorrenteRepository.class);
        useCase = new CriarLancamentoRecorrenteUseCase(repository);
    }

    @Test
    @DisplayName("executar: cria lancamento e chama salvar")
    void executarCriaEChama() {
        UUID contaId = UUID.randomUUID();
        LancamentoRecorrente esperado = new LancamentoRecorrente(
                "Aluguel", TipoTransacao.DESPESA,
                new Money(new BigDecimal("1500.00"), Currency.getInstance("BRL")),
                contaId, null, Periodicidade.MENSAL, LocalDate.of(2026, 6, 1));
        when(repository.salvar(any())).thenReturn(esperado);

        CriarLancamentoRecorrenteUseCase.Comando comando = new CriarLancamentoRecorrenteUseCase.Comando(
                "Aluguel", TipoTransacao.DESPESA, new BigDecimal("1500.00"), "BRL",
                contaId, null, Periodicidade.MENSAL, LocalDate.of(2026, 6, 1));

        LancamentoRecorrente resultado = useCase.executar(comando);

        assertThat(resultado).isEqualTo(esperado);
        verify(repository).salvar(any(LancamentoRecorrente.class));
    }

    @Test
    @DisplayName("executar: retorna instancia retornada pelo repositorio")
    void executarRetornaInstanciaDoRepositorio() {
        UUID contaId = UUID.randomUUID();
        LancamentoRecorrente mock = new LancamentoRecorrente(
                "Streaming", TipoTransacao.DESPESA,
                new Money(new BigDecimal("40.00"), Currency.getInstance("BRL")),
                contaId, null, Periodicidade.MENSAL, LocalDate.of(2026, 7, 1));
        when(repository.salvar(any())).thenReturn(mock);

        CriarLancamentoRecorrenteUseCase.Comando comando = new CriarLancamentoRecorrenteUseCase.Comando(
                "Streaming", TipoTransacao.DESPESA, new BigDecimal("40.00"), "BRL",
                contaId, null, Periodicidade.MENSAL, LocalDate.of(2026, 7, 1));

        LancamentoRecorrente resultado = useCase.executar(comando);

        assertThat(resultado).isSameAs(mock);
    }
}
