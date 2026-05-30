package com.laboratorio.financas.emprestimo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmprestimoTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Currency BRL = Currency.getInstance("BRL");

    private Money valor(String v) {
        return new Money(new BigDecimal(v), BRL);
    }

    @Test
    void construtorCriacaoComArgumentosValidosCriaEmprestimo() {
        Emprestimo e = new Emprestimo(
                USER_ID, "Emprestimo a Joao", "Joao Silva",
                TipoEmprestimo.CONCEDIDO, valor("500.00"), LocalDate.of(2026, 5, 30));

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestimo a Joao");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao Silva");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.getValor().valor()).isEqualByComparingTo("500.00");
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isNotNull();
        assertThat(e.getAtualizadoEm()).isNotNull();
    }

    @Test
    void construtorCriacaoNomeTerceiroVazioNormalizadoParaNull() {
        Emprestimo e = new Emprestimo(USER_ID, "X", "  ",
                TipoEmprestimo.RECEBIDO, valor("10.00"), LocalDate.now());
        assertThat(e.getNomeTerceiro()).isNull();
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException().isThrownBy(() -> new Emprestimo(
                null, "X", null, TipoEmprestimo.CONCEDIDO, valor("1.00"), LocalDate.now()));
    }

    @Test
    void construtorCriacaoComDescricaoNulaLancaNullPointerException() {
        assertThatNullPointerException().isThrownBy(() -> new Emprestimo(
                USER_ID, null, null, TipoEmprestimo.CONCEDIDO, valor("1.00"), LocalDate.now()));
    }

    @Test
    void construtorCriacaoComDescricaoVaziaLancaIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Emprestimo(
                USER_ID, "   ", null, TipoEmprestimo.CONCEDIDO, valor("1.00"), LocalDate.now()))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoMuitoLongaLancaIllegalArgumentException() {
        String longa = "x".repeat(101);
        assertThatIllegalArgumentException().isThrownBy(() -> new Emprestimo(
                USER_ID, longa, null, TipoEmprestimo.CONCEDIDO, valor("1.00"), LocalDate.now()))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComNomeTerceiroMuitoLongoLancaIllegalArgumentException() {
        String longa = "x".repeat(101);
        assertThatIllegalArgumentException().isThrownBy(() -> new Emprestimo(
                USER_ID, "ok", longa, TipoEmprestimo.CONCEDIDO, valor("1.00"), LocalDate.now()))
                .withMessageContaining("nomeTerceiro");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException().isThrownBy(() -> new Emprestimo(
                USER_ID, "ok", null, null, valor("1.00"), LocalDate.now()));
    }

    @Test
    void construtorCriacaoComValorNuloLancaNullPointerException() {
        assertThatNullPointerException().isThrownBy(() -> new Emprestimo(
                USER_ID, "ok", null, TipoEmprestimo.CONCEDIDO, null, LocalDate.now()));
    }

    @Test
    void construtorCriacaoComValorZeroLancaIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Emprestimo(
                USER_ID, "ok", null, TipoEmprestimo.CONCEDIDO, valor("0.00"), LocalDate.now()))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComValorNegativoLancaIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Emprestimo(
                USER_ID, "ok", null, TipoEmprestimo.CONCEDIDO, valor("-1.00"), LocalDate.now()))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComDataNulaLancaNullPointerException() {
        assertThatNullPointerException().isThrownBy(() -> new Emprestimo(
                USER_ID, "ok", null, TipoEmprestimo.CONCEDIDO, valor("1.00"), null));
    }

    @Test
    void atualizarMudaCamposEAtualizaTimestamp() throws InterruptedException {
        Emprestimo e = new Emprestimo(USER_ID, "Antiga", "Joao",
                TipoEmprestimo.CONCEDIDO, valor("100.00"), LocalDate.of(2026, 1, 1));
        Thread.sleep(5);

        e.atualizar("Nova", "Maria", TipoEmprestimo.RECEBIDO,
                valor("200.00"), LocalDate.of(2026, 2, 1), true);

        assertThat(e.getDescricao()).isEqualTo("Nova");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(e.getValor().valor()).isEqualByComparingTo("200.00");
        assertThat(e.getDataEmprestimo()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(e.isQuitado()).isTrue();
    }

    @Test
    void atualizarComValorZeroLancaIllegalArgumentException() {
        Emprestimo e = new Emprestimo(USER_ID, "X", null,
                TipoEmprestimo.CONCEDIDO, valor("100.00"), LocalDate.now());
        assertThatIllegalArgumentException().isThrownBy(() -> e.atualizar(
                "X", null, TipoEmprestimo.CONCEDIDO, valor("0.00"), LocalDate.now(), false))
                .withMessageContaining("valor");
    }

    @Test
    void emprestimosComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Emprestimo a = new Emprestimo(id, USER_ID, "A", null, TipoEmprestimo.CONCEDIDO,
                valor("1.00"), LocalDate.now(), false, java.time.Instant.now(), java.time.Instant.now());
        Emprestimo b = new Emprestimo(id, USER_ID, "B", null, TipoEmprestimo.RECEBIDO,
                valor("2.00"), LocalDate.now(), true, java.time.Instant.now(), java.time.Instant.now());
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
