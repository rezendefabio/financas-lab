package com.laboratorio.financas.conta.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContaTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money SALDO_ZERO = new Money(BigDecimal.ZERO, BRL);
    private static final Money SALDO_100 = new Money(new BigDecimal("100.00"), BRL);
    private static final Money SALDO_NEGATIVO = new Money(new BigDecimal("-50.00"), BRL);

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaContaAtiva() {
        // Given
        Instant antes = Instant.now();

        // When
        Conta conta = new Conta("Conta Corrente", TipoConta.CORRENTE, SALDO_100);

        // Then
        Instant depois = Instant.now();
        assertThat(conta.getId()).isNotNull();
        assertThat(conta.isAtiva()).isTrue();
        assertThat(conta.getCriadoEm()).isBetween(antes, depois);
        assertThat(conta.getAtualizadoEm()).isEqualTo(conta.getCriadoEm());
    }

    @Test
    void construtorNovoComNomeComEspacosNasPontasTrimaONome() {
        Conta conta = new Conta("  Poupanca  ", TipoConta.POUPANCA, SALDO_ZERO);
        assertThat(conta.getNome()).isEqualTo("Poupanca");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Conta(null, TipoConta.CORRENTE, SALDO_100))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeVazioLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Conta("", TipoConta.CORRENTE, SALDO_100))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeSomenteEspacosLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Conta("   ", TipoConta.CORRENTE, SALDO_100))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Conta(nomeLongo, TipoConta.CORRENTE, SALDO_100))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        Conta conta = new Conta(nome100, TipoConta.CORRENTE, SALDO_100);
        assertThat(conta.getNome()).hasSize(100);
    }

    @Test
    void construtorNovoComNome1CharAceita() {
        Conta conta = new Conta("X", TipoConta.DINHEIRO, SALDO_ZERO);
        assertThat(conta.getNome()).isEqualTo("X");
    }

    @Test
    void construtorNovoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Conta("Conta", null, SALDO_100))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorNovoComSaldoInicialNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Conta("Conta", TipoConta.CORRENTE, null))
                .withMessageContaining("saldoInicial");
    }

    @Test
    void construtorNovoAceitaSaldoPositivoZeroENegativo() {
        Conta positiva = new Conta("Corrente", TipoConta.CORRENTE, SALDO_100);
        Conta zero = new Conta("Dinheiro", TipoConta.DINHEIRO, SALDO_ZERO);
        Conta negativa = new Conta("Cartao", TipoConta.CARTAO_CREDITO, SALDO_NEGATIVO);

        assertThat(positiva.getSaldoInicial()).isEqualTo(SALDO_100);
        assertThat(zero.getSaldoInicial()).isEqualTo(SALDO_ZERO);
        assertThat(negativa.getSaldoInicial()).isEqualTo(SALDO_NEGATIVO);
    }

    @Test
    void doisContasCriadasEmSequenciaTenIdsDiferentes() {
        Conta conta1 = new Conta("Conta 1", TipoConta.CORRENTE, SALDO_ZERO);
        Conta conta2 = new Conta("Conta 2", TipoConta.CORRENTE, SALDO_ZERO);
        assertThat(conta1.getId()).isNotEqualTo(conta2.getId());
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoComTodosOsCamposValidosPreservaValores() {
        // Given
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-01-02T12:00:00Z");

        // When
        Conta conta = new Conta(id, "Poupanca", TipoConta.POUPANCA, SALDO_100, false, criadoEm, atualizadoEm);

        // Then
        assertThat(conta.getId()).isEqualTo(id);
        assertThat(conta.getNome()).isEqualTo("Poupanca");
        assertThat(conta.getTipo()).isEqualTo(TipoConta.POUPANCA);
        assertThat(conta.getSaldoInicial()).isEqualTo(SALDO_100);
        assertThat(conta.isAtiva()).isFalse();
        assertThat(conta.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(conta.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Conta(null, "Conta", TipoConta.CORRENTE, SALDO_100, true, Instant.now(), null))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Conta(UUID.randomUUID(), "Conta", TipoConta.CORRENTE, SALDO_100, true, null, null))
                .withMessageContaining("criadoEm");
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmNuloDefaultaParaCriadoEm() {
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Conta conta = new Conta(UUID.randomUUID(), "Conta", TipoConta.CORRENTE, SALDO_ZERO, true, criadoEm, null);
        assertThat(conta.getAtualizadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmDiferenteDeCriadoEmPreserva() {
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-06-15T08:30:00Z");
        Conta conta = new Conta(UUID.randomUUID(), "Conta", TipoConta.CORRENTE, SALDO_ZERO, true, criadoEm, atualizadoEm);
        assertThat(conta.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComAtivaFalsePreservaEstado() {
        Conta conta = new Conta(UUID.randomUUID(), "Conta", TipoConta.CORRENTE, SALDO_ZERO, false, Instant.now(), null);
        assertThat(conta.isAtiva()).isFalse();
    }

    // --- desativar() ---

    @Test
    void desativarContaAtivaRetornaNovaInstanciaInativa() {
        // Given
        Conta ativa = new Conta("Corrente", TipoConta.CORRENTE, SALDO_100);

        // When
        Conta inativa = ativa.desativar();

        // Then
        assertThat(inativa).isNotSameAs(ativa);
        assertThat(inativa.getId()).isEqualTo(ativa.getId());
        assertThat(inativa.isAtiva()).isFalse();
        assertThat(inativa.getAtualizadoEm()).isAfterOrEqualTo(ativa.getCriadoEm());
    }

    @Test
    void desativarContaJaInativaRetornaMesmaInstancia() {
        Conta ativa = new Conta("Corrente", TipoConta.CORRENTE, SALDO_100);
        Conta inativa = ativa.desativar();
        Conta resultado = inativa.desativar();
        assertThat(resultado).isSameAs(inativa);
    }

    @Test
    void desativarPreservaDemaisCampos() {
        // Given
        Conta ativa = new Conta("Minha Poupanca", TipoConta.POUPANCA, SALDO_100);

        // When
        Conta inativa = ativa.desativar();

        // Then
        assertThat(inativa.getNome()).isEqualTo("Minha Poupanca");
        assertThat(inativa.getTipo()).isEqualTo(TipoConta.POUPANCA);
        assertThat(inativa.getSaldoInicial()).isEqualTo(SALDO_100);
        assertThat(inativa.getCriadoEm()).isEqualTo(ativa.getCriadoEm());
    }

    // --- equals e hashCode ---

    @Test
    void duasContasComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Conta conta1 = new Conta(id, "Nome A", TipoConta.CORRENTE, SALDO_100, true, t, t);
        Conta conta2 = new Conta(id, "Nome B", TipoConta.POUPANCA, SALDO_ZERO, false, t, t);
        assertThat(conta1).isEqualTo(conta2);
    }

    @Test
    void duasContasComIdsDiferentesNaoSaoIguais() {
        Conta conta1 = new Conta("Conta 1", TipoConta.CORRENTE, SALDO_ZERO);
        Conta conta2 = new Conta("Conta 2", TipoConta.CORRENTE, SALDO_ZERO);
        assertThat(conta1).isNotEqualTo(conta2);
    }

    @Test
    void equalsRetornaFalseComparandoComNull() {
        Conta conta = new Conta("Conta", TipoConta.CORRENTE, SALDO_ZERO);
        assertThat(conta.equals(null)).isFalse();
    }

    @Test
    void equalsRetornaFalseComparandoComOutroTipo() {
        Conta conta = new Conta("Conta", TipoConta.CORRENTE, SALDO_ZERO);
        assertThat(conta.equals("string")).isFalse();
    }

    @Test
    void hashCodeConsistenteComEquals() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Conta conta1 = new Conta(id, "Nome A", TipoConta.CORRENTE, SALDO_100, true, t, t);
        Conta conta2 = new Conta(id, "Nome B", TipoConta.POUPANCA, SALDO_ZERO, false, t, t);
        assertThat(conta1.hashCode()).isEqualTo(conta2.hashCode());
    }

    // --- toString ---

    @Test
    void toStringContemIdNomeETipo() {
        Conta conta = new Conta("Corrente Principal", TipoConta.CORRENTE, SALDO_100);
        String resultado = conta.toString();
        assertThat(resultado)
                .contains(conta.getId().toString())
                .contains("Corrente Principal")
                .contains("CORRENTE");
    }

    @Test
    void toStringNaoContemSaldoNemTimestamps() {
        Conta conta = new Conta("Corrente", TipoConta.CORRENTE, SALDO_100);
        String resultado = conta.toString();
        assertThat(resultado)
                .doesNotContain("100")
                .doesNotContain("criadoEm")
                .doesNotContain("atualizadoEm");
    }
}
