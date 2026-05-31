package com.laboratorio.financas.orcamento.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import com.laboratorio.financas.orcamento.domain.StatusProgresso;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TransacaoCriadaEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrcamentoProgressoListenerTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate DATA = LocalDate.of(2026, 5, 17);
    private static final LocalDate MES = LocalDate.of(2026, 5, 1);

    private CalcularProgressoDoOrcamentoUseCase calcularProgresso;
    private OrcamentoRepository orcamentoRepository;
    private OrcamentoProgressoListener listener;

    @BeforeEach
    void setUp() {
        calcularProgresso = Mockito.mock(CalcularProgressoDoOrcamentoUseCase.class);
        orcamentoRepository = Mockito.mock(OrcamentoRepository.class);
        listener = new OrcamentoProgressoListener(calcularProgresso, orcamentoRepository);
    }

    @Test
    void eventoTipoReceitaEhIgnorado() {
        TransacaoCriadaEvent evento = evento(UUID.randomUUID(), "RECEITA");

        listener.onTransacaoCriada(evento);

        verify(orcamentoRepository, never()).listar();
        verify(calcularProgresso, never()).executar(any());
    }

    @Test
    void eventoTipoTransferenciaEhIgnorado() {
        TransacaoCriadaEvent evento = evento(UUID.randomUUID(), "TRANSFERENCIA");

        listener.onTransacaoCriada(evento);

        verify(orcamentoRepository, never()).listar();
        verify(calcularProgresso, never()).executar(any());
    }

    @Test
    void eventoDespesaSemCategoriaEhIgnorado() {
        TransacaoCriadaEvent evento = evento(null, "DESPESA");

        listener.onTransacaoCriada(evento);

        verify(orcamentoRepository, never()).listar();
        verify(calcularProgresso, never()).executar(any());
    }

    @Test
    void eventoDespesaSemOrcamentoAtivoNaoCalculaProgresso() {
        UUID categoriaId = UUID.randomUUID();
        TransacaoCriadaEvent evento = evento(categoriaId, "DESPESA");
        // Orcamento existe mas esta inativo
        Orcamento inativo = orcamento(categoriaId, MES, false);
        when(orcamentoRepository.listar()).thenReturn(List.of(inativo));

        listener.onTransacaoCriada(evento);

        verify(calcularProgresso, never()).executar(any());
    }

    @Test
    void eventoDespesaSemOrcamentoNaCategoriaNaoCalculaProgresso() {
        UUID categoriaId = UUID.randomUUID();
        TransacaoCriadaEvent evento = evento(categoriaId, "DESPESA");
        // Orcamento ativo mas de outra categoria
        Orcamento outraCategoria = orcamento(UUID.randomUUID(), MES, true);
        when(orcamentoRepository.listar()).thenReturn(List.of(outraCategoria));

        listener.onTransacaoCriada(evento);

        verify(calcularProgresso, never()).executar(any());
    }

    @Test
    void eventoDespesaComOrcamentoDeOutroMesNaoCalculaProgresso() {
        UUID categoriaId = UUID.randomUUID();
        TransacaoCriadaEvent evento = evento(categoriaId, "DESPESA");
        // Orcamento ativo, mesma categoria, mas mes diferente
        Orcamento outroMes = orcamento(categoriaId, LocalDate.of(2026, 4, 1), true);
        when(orcamentoRepository.listar()).thenReturn(List.of(outroMes));

        listener.onTransacaoCriada(evento);

        verify(calcularProgresso, never()).executar(any());
    }

    @Test
    void eventoDespesaComOrcamentoAtivoCalculaProgresso() {
        UUID categoriaId = UUID.randomUUID();
        TransacaoCriadaEvent evento = evento(categoriaId, "DESPESA");
        Orcamento ativo = orcamento(categoriaId, MES, true);
        when(orcamentoRepository.listar()).thenReturn(List.of(ativo));
        when(calcularProgresso.executar(ativo.getId()))
                .thenReturn(resultado(ativo, new BigDecimal("90.00"), StatusProgresso.ATENCAO));

        listener.onTransacaoCriada(evento);

        verify(calcularProgresso, times(1)).executar(ativo.getId());
    }

    @Test
    void eventoDespesaComProgressoAbaixoDe80NaoFalha() {
        UUID categoriaId = UUID.randomUUID();
        TransacaoCriadaEvent evento = evento(categoriaId, "DESPESA");
        Orcamento ativo = orcamento(categoriaId, MES, true);
        when(orcamentoRepository.listar()).thenReturn(List.of(ativo));
        when(calcularProgresso.executar(ativo.getId()))
                .thenReturn(resultado(ativo, new BigDecimal("50.00"), StatusProgresso.ABAIXO));

        listener.onTransacaoCriada(evento);

        verify(calcularProgresso, times(1)).executar(ativo.getId());
    }

    @Test
    void erroNoCalculoDeUmOrcamentoNaoImpedeOsDemais() {
        UUID categoriaId = UUID.randomUUID();
        TransacaoCriadaEvent evento = evento(categoriaId, "DESPESA");
        Orcamento primeiro = orcamento(categoriaId, MES, true);
        Orcamento segundo = orcamento(categoriaId, MES, true);
        when(orcamentoRepository.listar()).thenReturn(List.of(primeiro, segundo));
        when(calcularProgresso.executar(primeiro.getId()))
                .thenThrow(new RuntimeException("falha simulada"));
        when(calcularProgresso.executar(segundo.getId()))
                .thenReturn(resultado(segundo, new BigDecimal("90.00"), StatusProgresso.ATENCAO));

        listener.onTransacaoCriada(evento);

        // Apesar do erro no primeiro, o segundo ainda e processado.
        verify(calcularProgresso, times(1)).executar(primeiro.getId());
        verify(calcularProgresso, times(1)).executar(segundo.getId());
    }

    private TransacaoCriadaEvent evento(UUID categoriaId, String tipo) {
        return new TransacaoCriadaEvent(
                UUID.randomUUID(),
                categoriaId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                DATA,
                tipo);
    }

    private Orcamento orcamento(UUID categoriaId, LocalDate mesAno, boolean ativo) {
        return new Orcamento(UUID.randomUUID(), UUID.randomUUID(), categoriaId,
                new Money(new BigDecimal("1000.00"), BRL),
                mesAno, ativo, Instant.now(), Instant.now());
    }

    private CalcularProgressoDoOrcamentoUseCase.Resultado resultado(Orcamento orcamento,
                                                                    BigDecimal percentual,
                                                                    StatusProgresso status) {
        return new CalcularProgressoDoOrcamentoUseCase.Resultado(
                orcamento.getId(),
                orcamento.getCategoriaId(),
                orcamento.getMesAno(),
                orcamento.getValorLimite(),
                new Money(BigDecimal.ZERO, BRL),
                percentual,
                status);
    }
}
