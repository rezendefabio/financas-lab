package com.laboratorio.financas.conta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CalcularSaldoTotalUseCaseTest {

    private ContaRepository contaRepository;
    private CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase;
    private CalcularSaldoTotalUseCase useCase;

    private static final Currency BRL = Currency.getInstance("BRL");

    @BeforeEach
    void setUp() {
        contaRepository = Mockito.mock(ContaRepository.class);
        calcularSaldoDaContaUseCase = Mockito.mock(CalcularSaldoDaContaUseCase.class);
        useCase = new CalcularSaldoTotalUseCase(contaRepository, calcularSaldoDaContaUseCase);
    }

    private Conta contaFake(UUID id) {
        Money saldoZero = new Money(BigDecimal.ZERO, BRL);
        return new Conta(
                id, UUID.randomUUID(), "Conta", com.laboratorio.financas.conta.domain.TipoConta.CORRENTE,
                saldoZero, saldoZero, null,
                null, null, true, Instant.now(), null
        );
    }

    private CalcularSaldoDaContaUseCase.Resultado resultadoFake(UUID contaId, BigDecimal saldo) {
        Money zero = new Money(BigDecimal.ZERO, BRL);
        return new CalcularSaldoDaContaUseCase.Resultado(
                contaId, zero, zero, zero, zero, zero,
                new Money(saldo, BRL), Instant.now()
        );
    }

    @Test
    void executarSemContasAtivasRetornaZero() {
        when(contaRepository.listarAtivas()).thenReturn(List.of());

        CalcularSaldoTotalUseCase.Resultado resultado = useCase.executar();

        assertThat(resultado.totalContas()).isZero();
        assertThat(resultado.saldoTotal().valor()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void executarComUmaContaRetornaSaldoDessa() {
        UUID id = UUID.randomUUID();
        Conta conta = contaFake(id);
        when(contaRepository.listarAtivas()).thenReturn(List.of(conta));
        when(calcularSaldoDaContaUseCase.executar(id)).thenReturn(resultadoFake(id, new BigDecimal("500.00")));

        CalcularSaldoTotalUseCase.Resultado resultado = useCase.executar();

        assertThat(resultado.totalContas()).isEqualTo(1);
        assertThat(resultado.saldoTotal().valor()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void executarComDuasContasSomaOsSaldos() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(contaRepository.listarAtivas()).thenReturn(List.of(contaFake(id1), contaFake(id2)));
        when(calcularSaldoDaContaUseCase.executar(id1)).thenReturn(resultadoFake(id1, new BigDecimal("1000.00")));
        when(calcularSaldoDaContaUseCase.executar(id2)).thenReturn(resultadoFake(id2, new BigDecimal("250.50")));

        CalcularSaldoTotalUseCase.Resultado resultado = useCase.executar();

        assertThat(resultado.totalContas()).isEqualTo(2);
        assertThat(resultado.saldoTotal().valor()).isEqualByComparingTo(new BigDecimal("1250.50"));
    }

    @Test
    void executarRetornaMoedaBrl() {
        when(contaRepository.listarAtivas()).thenReturn(List.of());

        CalcularSaldoTotalUseCase.Resultado resultado = useCase.executar();

        assertThat(resultado.saldoTotal().moeda()).isEqualTo(BRL);
    }
}
