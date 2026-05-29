package com.laboratorio.financas.limite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LimiteTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Currency BRL = Currency.getInstance("BRL");

    private static Money valor(String valor) {
        return new Money(new BigDecimal(valor), BRL);
    }

    @Test
    void construtorCriacaoComArgumentosValidosCriaLimite() {
        Instant antes = Instant.now();
        Limite limite = new Limite(USER_ID, "Limite Mensal", TipoLimite.MENSAL, valor("500.00"));
        Instant depois = Instant.now();

        assertThat(limite.getId()).isNotNull();
        assertThat(limite.getUserId()).isEqualTo(USER_ID);
        assertThat(limite.getNome()).isEqualTo("Limite Mensal");
        assertThat(limite.getTipo()).isEqualTo(TipoLimite.MENSAL);
        assertThat(limite.getValor().valor()).isEqualByComparingTo("500.00");
        assertThat(limite.isAtivo()).isTrue();
        assertThat(limite.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Limite(null, "Nome", TipoLimite.DIARIO, valor("10.00")))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Limite(USER_ID, null, TipoLimite.DIARIO, valor("10.00")))
                .withMessageContaining("nome");
    }

    @Test
    void construtorCriacaoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Limite(USER_ID, "   ", TipoLimite.DIARIO, valor("10.00")))
                .withMessageContaining("nome");
    }

    @Test
    void construtorCriacaoComNomeAcimaDe100CaracteresLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Limite(USER_ID, nomeLongo, TipoLimite.DIARIO, valor("10.00")))
                .withMessageContaining("100");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Limite(USER_ID, "Nome", null, valor("10.00")))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComValorNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Limite(USER_ID, "Nome", TipoLimite.DIARIO, null))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComValorZeroLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Limite(USER_ID, "Nome", TipoLimite.DIARIO, valor("0.00")))
                .withMessageContaining("positivo");
    }

    @Test
    void construtorCriacaoComValorNegativoLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Limite(USER_ID, "Nome", TipoLimite.DIARIO, valor("-1.00")))
                .withMessageContaining("positivo");
    }

    @Test
    void desativarMudaAtivoParaFalseEAtualizaTimestamp() {
        Limite limite = new Limite(USER_ID, "Nome", TipoLimite.ANUAL, valor("100.00"));
        Instant antes = limite.getAtualizadoEm();

        limite.desativar();

        assertThat(limite.isAtivo()).isFalse();
        assertThat(limite.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void atualizarComArgumentosValidosAlteraCampos() {
        Limite limite = new Limite(USER_ID, "Antigo", TipoLimite.DIARIO, valor("50.00"));

        limite.atualizar("Novo", TipoLimite.SEMANAL, valor("75.50"));

        assertThat(limite.getNome()).isEqualTo("Novo");
        assertThat(limite.getTipo()).isEqualTo(TipoLimite.SEMANAL);
        assertThat(limite.getValor().valor()).isEqualByComparingTo("75.50");
    }

    @Test
    void atualizarComValorInvalidoLancaIllegalArgumentException() {
        Limite limite = new Limite(USER_ID, "Antigo", TipoLimite.DIARIO, valor("50.00"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> limite.atualizar("Novo", TipoLimite.DIARIO, valor("0.00")))
                .withMessageContaining("positivo");
    }

    @Test
    void atualizarComTipoNuloLancaNullPointerException() {
        Limite limite = new Limite(USER_ID, "Antigo", TipoLimite.DIARIO, valor("50.00"));

        assertThatNullPointerException()
                .isThrownBy(() -> limite.atualizar("Novo", null, valor("10.00")))
                .withMessageContaining("tipo");
    }
}
