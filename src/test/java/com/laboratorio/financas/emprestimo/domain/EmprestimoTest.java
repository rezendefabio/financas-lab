package com.laboratorio.financas.emprestimo.domain;

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

class EmprestimoTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Currency BRL = Currency.getInstance("BRL");

    private Money money(String valor) {
        return new Money(new BigDecimal(valor), BRL);
    }

    @Test
    void construtorCriacaoComArgumentosValidosCriaEntidade() {
        Instant antes = Instant.now();
        Emprestimo e = new Emprestimo(
                USER_ID, "Emprestimo X", "Joao", TipoEmprestimo.CONCEDIDO,
                money("100.00"), LocalDate.now(), false);
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestimo X");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComNomeTerceiroVazioNormalizaParaNull() {
        Emprestimo e = new Emprestimo(
                USER_ID, "X", "   ", TipoEmprestimo.RECEBIDO,
                money("10.00"), LocalDate.now(), false);
        assertThat(e.getNomeTerceiro()).isNull();
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(
                        null, "X", null, TipoEmprestimo.CONCEDIDO,
                        money("10.00"), LocalDate.now(), false))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComDescricaoBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(
                        USER_ID, "   ", null, TipoEmprestimo.CONCEDIDO,
                        money("10.00"), LocalDate.now(), false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(
                        USER_ID, "X", null, null,
                        money("10.00"), LocalDate.now(), false))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComValorZeroLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(
                        USER_ID, "X", null, TipoEmprestimo.CONCEDIDO,
                        money("0.00"), LocalDate.now(), false))
                .withMessageContaining("valor");
    }

    @Test
    void atualizarMudaCamposEAtualizaTimestamp() {
        Emprestimo e = new Emprestimo(
                USER_ID, "Antigo", null, TipoEmprestimo.CONCEDIDO,
                money("10.00"), LocalDate.now(), false);
        Instant antes = e.getAtualizadoEm();

        e.atualizar("Novo", "Maria", TipoEmprestimo.RECEBIDO,
                money("20.00"), LocalDate.now(), true);

        assertThat(e.getDescricao()).isEqualTo("Novo");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }
}
