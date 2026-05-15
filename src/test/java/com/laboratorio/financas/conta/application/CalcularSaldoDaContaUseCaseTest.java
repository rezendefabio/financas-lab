package com.laboratorio.financas.conta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CalcularSaldoDaContaUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money SALDO_INICIAL_1000 = new Money(BigDecimal.valueOf(1000), BRL);

    private ContaRepository contaRepository;
    private TransacaoRepository transacaoRepository;
    private CalcularSaldoDaContaUseCase useCase;

    @BeforeEach
    void setUp() {
        contaRepository = Mockito.mock(ContaRepository.class);
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        useCase = new CalcularSaldoDaContaUseCase(contaRepository, transacaoRepository);
    }

    @Test
    void contaExisteSemTransacoesRetornaSaldoIgualAoInicial() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        TotaisTransacaoPorConta totaisZerados = totais(0, 0, 0, 0);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totaisZerados);

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        assertThat(resultado.saldoAtual().valor()).isEqualByComparingTo("1000.00");
        assertThat(resultado.totalReceitas().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalTransferenciasEnviadas().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalTransferenciasRecebidas().valor()).isEqualByComparingTo("0.00");
    }

    @Test
    void contaExisteComReceita500RetornaSaldoInicialMaisReceita() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        TotaisTransacaoPorConta totais = totais(500, 0, 0, 0);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais);

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        assertThat(resultado.saldoAtual().valor()).isEqualByComparingTo("1500.00");
    }

    @Test
    void contaExisteComDespesa200RetornaSaldoInicialMenosDespesa() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        TotaisTransacaoPorConta totais = totais(0, 200, 0, 0);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais);

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        assertThat(resultado.saldoAtual().valor()).isEqualByComparingTo("800.00");
    }

    @Test
    void contaExisteComTransferenciaEnviada100RetornaSaldoInicialMenosTransferencia() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        TotaisTransacaoPorConta totais = totais(0, 0, 100, 0);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais);

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        assertThat(resultado.saldoAtual().valor()).isEqualByComparingTo("900.00");
    }

    @Test
    void contaExisteComTransferenciaRecebida100RetornaSaldoInicialMaisTransferencia() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        TotaisTransacaoPorConta totais = totais(0, 0, 0, 100);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais);

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        assertThat(resultado.saldoAtual().valor()).isEqualByComparingTo("1100.00");
    }

    @Test
    void cenarioMistoCalculaSaldoCorretamente() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        TotaisTransacaoPorConta totais = totais(800, 200, 100, 50);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais);

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        // 1000 + 800 - 200 - 100 + 50 = 1550
        assertThat(resultado.saldoAtual().valor()).isEqualByComparingTo("1550.00");
        assertThat(resultado.totalReceitas().valor()).isEqualByComparingTo("800.00");
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("200.00");
        assertThat(resultado.totalTransferenciasEnviadas().valor()).isEqualByComparingTo("100.00");
        assertThat(resultado.totalTransferenciasRecebidas().valor()).isEqualByComparingTo("50.00");
    }

    @Test
    void contaInexistenteLancaContaNaoEncontradaException() {
        // Given
        UUID contaId = UUID.randomUUID();
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(contaId))
                .isInstanceOf(ContaNaoEncontradaException.class)
                .satisfies(ex -> {
                    ContaNaoEncontradaException cnee = (ContaNaoEncontradaException) ex;
                    assertThat(cnee.getId()).isEqualTo(contaId);
                });
    }

    @Test
    void calculadoEmEstaEmJanelaRazoavel() {
        // Given
        UUID contaId = UUID.randomUUID();
        Instant antes = Instant.now();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais(0, 0, 0, 0));

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        assertThat(resultado.calculadoEm()).isCloseTo(antes, within(5, ChronoUnit.SECONDS));
    }

    @Test
    void moedaEPreservadaParaTodosOsMoneys() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais(100, 50, 30, 20));

        // When
        CalcularSaldoDaContaUseCase.Resultado resultado = useCase.executar(contaId);

        // Then
        assertThat(resultado.saldoInicial().moeda()).isEqualTo(BRL);
        assertThat(resultado.totalReceitas().moeda()).isEqualTo(BRL);
        assertThat(resultado.totalDespesas().moeda()).isEqualTo(BRL);
        assertThat(resultado.totalTransferenciasEnviadas().moeda()).isEqualTo(BRL);
        assertThat(resultado.totalTransferenciasRecebidas().moeda()).isEqualTo(BRL);
        assertThat(resultado.saldoAtual().moeda()).isEqualTo(BRL);
    }

    @Test
    void calcularTotaisPorContaEChamadoUmaVezComContaIdCorreto() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaComSaldo(contaId, SALDO_INICIAL_1000);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.calcularTotaisPorConta(contaId)).thenReturn(totais(0, 0, 0, 0));

        // When
        useCase.executar(contaId);

        // Then
        verify(transacaoRepository, times(1)).calcularTotaisPorConta(contaId);
    }

    private Conta contaComSaldo(UUID id, Money saldoInicial) {
        return new Conta(
                id,
                null,
                "Conta Teste",
                TipoConta.CORRENTE,
                saldoInicial,
                saldoInicial,
                null,
                null,
                null,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private TotaisTransacaoPorConta totais(
            double receitas,
            double despesas,
            double transferenciasEnviadas,
            double transferenciasRecebidas
    ) {
        return new TotaisTransacaoPorConta(
                BigDecimal.valueOf(receitas),
                BigDecimal.valueOf(despesas),
                BigDecimal.valueOf(transferenciasEnviadas),
                BigDecimal.valueOf(transferenciasRecebidas)
        );
    }
}
