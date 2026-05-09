package com.laboratorio.financas.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    // ─── Construtor ──────────────────────────────────────────────────────────

    @Test
    void construtorComValorEMoedaValidosCriaMoney() {
        Money money = new Money(new BigDecimal("10.00"), BRL);

        assertThat(money.valor()).isEqualByComparingTo("10.00");
        assertThat(money.moeda()).isEqualTo(BRL);
    }

    @Test
    void construtorComValorNuloLancaNullPointerException() {
        assertThatThrownBy(() -> new Money(null, BRL))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("valor");
    }

    @Test
    void construtorComMoedaNulaLancaNullPointerException() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10.00"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("moeda");
    }

    @Test
    void construtorComEscalaZeroNormalizaParaDois() {
        Money money = new Money(new BigDecimal("10"), BRL);

        assertThat(money.valor()).isEqualByComparingTo("10.00");
        assertThat(money.valor().scale()).isEqualTo(2);
    }

    @Test
    void construtorBankersRoundingCasoParArredondaPraBaixo() {
        // 10.005 com HALF_EVEN: digito antes do 5 e 0 (par), arredonda pra baixo -> 10.00
        Money money = new Money(new BigDecimal("10.005"), BRL);

        assertThat(money.valor()).isEqualByComparingTo("10.00");
        assertThat(money.valor().scale()).isEqualTo(2);
    }

    @Test
    void construtorBankersRoundingCasoImparArredondaPraCima() {
        // 10.015 com HALF_EVEN: digito antes do 5 e 1 (impar), arredonda pra cima -> 10.02
        Money money = new Money(new BigDecimal("10.015"), BRL);

        assertThat(money.valor()).isEqualByComparingTo("10.02");
        assertThat(money.valor().scale()).isEqualTo(2);
    }

    @Test
    void construtorComValorNegativoCriaMoney() {
        Money money = new Money(new BigDecimal("-50.00"), BRL);

        assertThat(money.valor()).isEqualByComparingTo("-50.00");
        assertThat(money.ehNegativo()).isTrue();
    }

    @Test
    void construtorComValorZeroCriaMoney() {
        Money money = new Money(BigDecimal.ZERO, BRL);

        assertThat(money.ehZero()).isTrue();
    }

    // ─── somar ───────────────────────────────────────────────────────────────

    @Test
    void somarDoisValoresPositivosMesmaMoedaRetornaSoma() {
        // Given
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money b = new Money(new BigDecimal("5.50"), BRL);

        // When
        Money resultado = a.somar(b);

        // Then
        assertThat(resultado.valor()).isEqualByComparingTo("15.50");
        assertThat(resultado.moeda()).isEqualTo(BRL);
    }

    @Test
    void somarPositivoComNegativoResultaPositivo() {
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money b = new Money(new BigDecimal("-3.00"), BRL);

        Money resultado = a.somar(b);

        assertThat(resultado.valor()).isEqualByComparingTo("7.00");
    }

    @Test
    void somarPositivoComNegativoResultaNegativo() {
        Money a = new Money(new BigDecimal("3.00"), BRL);
        Money b = new Money(new BigDecimal("-10.00"), BRL);

        Money resultado = a.somar(b);

        assertThat(resultado.valor()).isEqualByComparingTo("-7.00");
    }

    @Test
    void somarComZeroRetornaMesmoValor() {
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money zero = new Money(BigDecimal.ZERO, BRL);

        Money resultado = a.somar(zero);

        assertThat(resultado.valor()).isEqualByComparingTo("10.00");
    }

    @Test
    void somarMoedasDiferentesLancaIllegalArgumentException() {
        // Given
        Money brl = new Money(new BigDecimal("10.00"), BRL);
        Money usd = new Money(new BigDecimal("10.00"), USD);

        // When / Then
        assertThatThrownBy(() -> brl.somar(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BRL")
                .hasMessageContaining("USD");
    }

    @Test
    void somarOutroNuloLancaNullPointerException() {
        Money a = new Money(new BigDecimal("10.00"), BRL);

        assertThatThrownBy(() -> a.somar(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("outro");
    }

    // ─── subtrair ────────────────────────────────────────────────────────────

    @Test
    void subtrairDoisValoresPositivosRetornaDiferenca() {
        // Given
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money b = new Money(new BigDecimal("3.00"), BRL);

        // When
        Money resultado = a.subtrair(b);

        // Then
        assertThat(resultado.valor()).isEqualByComparingTo("7.00");
    }

    @Test
    void subtrairResultandoEmNegativoRetornaNegativo() {
        Money a = new Money(new BigDecimal("3.00"), BRL);
        Money b = new Money(new BigDecimal("10.00"), BRL);

        Money resultado = a.subtrair(b);

        assertThat(resultado.valor()).isEqualByComparingTo("-7.00");
    }

    @Test
    void subtrairZeroRetornaMesmoValor() {
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money zero = new Money(BigDecimal.ZERO, BRL);

        Money resultado = a.subtrair(zero);

        assertThat(resultado.valor()).isEqualByComparingTo("10.00");
    }

    @Test
    void subtrairMoedasDiferentesLancaIllegalArgumentException() {
        Money brl = new Money(new BigDecimal("10.00"), BRL);
        Money usd = new Money(new BigDecimal("5.00"), USD);

        assertThatThrownBy(() -> brl.subtrair(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BRL")
                .hasMessageContaining("USD");
    }

    // ─── multiplicar ─────────────────────────────────────────────────────────

    @Test
    void multiplicarPorInteiroPositivoRetornaProduto() {
        // Given
        Money a = new Money(new BigDecimal("10.00"), BRL);

        // When
        Money resultado = a.multiplicar(new BigDecimal("3"));

        // Then
        assertThat(resultado.valor()).isEqualByComparingTo("30.00");
    }

    @Test
    void multiplicarPorFracaoDezPorcentoRetornaDezPorcento() {
        Money a = new Money(new BigDecimal("50.00"), BRL);

        Money resultado = a.multiplicar(new BigDecimal("0.10"));

        assertThat(resultado.valor()).isEqualByComparingTo("5.00");
    }

    @Test
    void multiplicarPorZeroRetornaZero() {
        Money a = new Money(new BigDecimal("10.00"), BRL);

        Money resultado = a.multiplicar(BigDecimal.ZERO);

        assertThat(resultado.ehZero()).isTrue();
    }

    @Test
    void multiplicarPorNegativoInverteSignal() {
        Money a = new Money(new BigDecimal("10.00"), BRL);

        Money resultado = a.multiplicar(new BigDecimal("-1"));

        assertThat(resultado.valor()).isEqualByComparingTo("-10.00");
    }

    @Test
    void multiplicarGeraEscalaMaiorNormalizaParaDois() {
        // 10.00 * 0.333 = 3.330 -> normalizado para 3.33
        Money a = new Money(new BigDecimal("10.00"), BRL);

        Money resultado = a.multiplicar(new BigDecimal("0.333"));

        assertThat(resultado.valor().scale()).isEqualTo(2);
        assertThat(resultado.valor()).isEqualByComparingTo("3.33");
    }

    @Test
    void multiplicarFatorNuloLancaNullPointerException() {
        Money a = new Money(new BigDecimal("10.00"), BRL);

        assertThatThrownBy(() -> a.multiplicar(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fator");
    }

    // ─── negar ───────────────────────────────────────────────────────────────

    @Test
    void negarPositivoRetornaNegativo() {
        Money a = new Money(new BigDecimal("10.00"), BRL);

        Money resultado = a.negar();

        assertThat(resultado.valor()).isEqualByComparingTo("-10.00");
    }

    @Test
    void negarNegativoRetornaPositivo() {
        Money a = new Money(new BigDecimal("-10.00"), BRL);

        Money resultado = a.negar();

        assertThat(resultado.valor()).isEqualByComparingTo("10.00");
    }

    @Test
    void negarZeroRetornaZero() {
        Money a = new Money(BigDecimal.ZERO, BRL);

        Money resultado = a.negar();

        assertThat(resultado.ehZero()).isTrue();
    }

    // ─── ehZero, ehNegativo, ehPositivo ──────────────────────────────────────

    @Test
    void ehZeroValorZeroRetornaTrue() {
        Money money = new Money(new BigDecimal("0.00"), BRL);

        assertThat(money.ehZero()).isTrue();
    }

    @Test
    void ehZeroValorPositivoRetornaFalse() {
        assertThat(new Money(new BigDecimal("0.01"), BRL).ehZero()).isFalse();
    }

    @Test
    void ehZeroValorNegativoRetornaFalse() {
        assertThat(new Money(new BigDecimal("-0.01"), BRL).ehZero()).isFalse();
    }

    @Test
    void ehNegativoValorNegativoRetornaTrue() {
        assertThat(new Money(new BigDecimal("-1.00"), BRL).ehNegativo()).isTrue();
    }

    @Test
    void ehNegativoValorZeroRetornaFalse() {
        assertThat(new Money(new BigDecimal("0.00"), BRL).ehNegativo()).isFalse();
    }

    @Test
    void ehNegativoValorPositivoRetornaFalse() {
        assertThat(new Money(new BigDecimal("1.00"), BRL).ehNegativo()).isFalse();
    }

    @Test
    void ehPositivoValorPositivoRetornaTrue() {
        assertThat(new Money(new BigDecimal("1.00"), BRL).ehPositivo()).isTrue();
    }

    @Test
    void ehPositivoValorZeroRetornaFalse() {
        assertThat(new Money(new BigDecimal("0.00"), BRL).ehPositivo()).isFalse();
    }

    @Test
    void ehPositivoValorNegativoRetornaFalse() {
        assertThat(new Money(new BigDecimal("-1.00"), BRL).ehPositivo()).isFalse();
    }

    // ─── Igualdade (record) ───────────────────────────────────────────────────

    @Test
    void equalsComMesmoValorEMoedaRetornaTrue() {
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money b = new Money(new BigDecimal("10.00"), BRL);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equalsComValoresDiferentesRetornaFalse() {
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money b = new Money(new BigDecimal("20.00"), BRL);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsComMoedasDiferentesRetornaFalse() {
        Money a = new Money(new BigDecimal("10.00"), BRL);
        Money b = new Money(new BigDecimal("10.00"), USD);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsEscalasDiferentesNormalizadasRetornaTrue() {
        // Apos normalizacao ambos ficam com valor 10.00 e escala 2
        Money a = new Money(new BigDecimal("10"), BRL);
        Money b = new Money(new BigDecimal("10.00"), BRL);

        assertThat(a).isEqualTo(b);
    }
}
