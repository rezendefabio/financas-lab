package com.laboratorio.financas.lancamentorecorrente.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarLancamentosRecorrentesUseCaseTest {

    private LancamentoRecorrenteRepository repository;
    private ListarLancamentosRecorrentesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LancamentoRecorrenteRepository.class);
        useCase = new ListarLancamentosRecorrentesUseCase(repository);
    }

    @Test
    @DisplayName("executar: retorna lista do repositorio")
    void executarRetornaLista() {
        LancamentoRecorrente l = new LancamentoRecorrente(
                "Mensal", TipoTransacao.DESPESA,
                new Money(new BigDecimal("100.00"), Currency.getInstance("BRL")),
                UUID.randomUUID(), null, Periodicidade.MENSAL, LocalDate.now());
        when(repository.listar()).thenReturn(List.of(l));

        List<LancamentoRecorrente> resultado = useCase.executar();

        assertThat(resultado).hasSize(1).contains(l);
    }

    @Test
    @DisplayName("executar: retorna lista vazia quando repositorio vazio")
    void executarRetornaListaVazia() {
        when(repository.listar()).thenReturn(List.of());

        List<LancamentoRecorrente> resultado = useCase.executar();

        assertThat(resultado).isEmpty();
    }
}
