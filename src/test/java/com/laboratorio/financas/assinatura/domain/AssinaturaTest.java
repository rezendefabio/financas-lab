package com.laboratorio.financas.assinatura.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssinaturaTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money VALOR = new Money(new BigDecimal("29.90"), BRL);
    private static final LocalDate RENOVACAO = LocalDate.of(2026, 6, 15);

    @Test
    void construtorCriacaoComArgumentosValidosCriaEntidade() {
        Instant antes = Instant.now();
        Assinatura entidade = new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        Instant depois = Instant.now();

        assertThat(entidade.getId()).isNotNull();
        assertThat(entidade.getUserId()).isEqualTo(USER_ID);
        assertThat(entidade.getNome()).isEqualTo("Netflix");
        assertThat(entidade.getTipo()).isEqualTo(TipoAssinatura.STREAMING);
        assertThat(entidade.getValorMensal()).isEqualTo(VALOR);
        assertThat(entidade.getDataRenovacao()).isEqualTo(RENOVACAO);
        assertThat(entidade.isAtiva()).isTrue();
        assertThat(entidade.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Assinatura(null, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Assinatura(USER_ID, null, TipoAssinatura.STREAMING, VALOR, RENOVACAO))
                .withMessageContaining("nome");
    }

    @Test
    void construtorCriacaoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Assinatura(USER_ID, "  ", TipoAssinatura.STREAMING, VALOR, RENOVACAO))
                .withMessageContaining("nome");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Assinatura(USER_ID, "Netflix", null, VALOR, RENOVACAO))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComValorNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, null, RENOVACAO))
                .withMessageContaining("valorMensal");
    }

    @Test
    void construtorCriacaoComValorNaoPositivoLancaIllegalArgumentException() {
        Money zero = new Money(BigDecimal.ZERO, BRL);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, zero, RENOVACAO))
                .withMessageContaining("valorMensal");
    }

    @Test
    void construtorCriacaoComDataRenovacaoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, null))
                .withMessageContaining("dataRenovacao");
    }

    @Test
    void desativarMudaAtivaParaFalseEAtualizaTimestamp() {
        Assinatura entidade = new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        Instant antes = entidade.getAtualizadoEm();

        entidade.desativar();

        assertThat(entidade.isAtiva()).isFalse();
        assertThat(entidade.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void atualizarMudaTodosOsCamposMutaveis() {
        Assinatura entidade = new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        Money novoValor = new Money(new BigDecimal("49.90"), BRL);
        LocalDate novaData = LocalDate.of(2026, 7, 1);

        entidade.atualizar("Spotify", TipoAssinatura.OUTROS, novoValor, novaData, false);

        assertThat(entidade.getNome()).isEqualTo("Spotify");
        assertThat(entidade.getTipo()).isEqualTo(TipoAssinatura.OUTROS);
        assertThat(entidade.getValorMensal()).isEqualTo(novoValor);
        assertThat(entidade.getDataRenovacao()).isEqualTo(novaData);
        assertThat(entidade.isAtiva()).isFalse();
    }
}
