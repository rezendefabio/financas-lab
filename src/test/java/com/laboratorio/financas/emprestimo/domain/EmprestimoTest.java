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
    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

    private static Money valor(String v) {
        return new Money(new BigDecimal(v), BRL);
    }

    @Test
    void construtorCriacaoComArgumentosValidosCriaEmprestimo() {
        Instant antes = Instant.now();
        Emprestimo e = new Emprestimo(USER_ID, "Emprestimo ao Joao", "Joao",
                TipoEmprestimo.CONCEDIDO, valor("500.00"), DATA, false);
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestimo ao Joao");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.getValor().valor()).isEqualByComparingTo("500.00");
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA);
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComNomeTerceiroNuloEhPermitido() {
        Emprestimo e = new Emprestimo(USER_ID, "Sem terceiro", null,
                TipoEmprestimo.RECEBIDO, valor("10.00"), DATA, false);
        assertThat(e.getNomeTerceiro()).isNull();
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(null, "Desc", null,
                        TipoEmprestimo.CONCEDIDO, valor("1.00"), DATA, false))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComDescricaoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, null, null,
                        TipoEmprestimo.CONCEDIDO, valor("1.00"), DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "   ", null,
                        TipoEmprestimo.CONCEDIDO, valor("1.00"), DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoAcimaDe100LancaIllegalArgumentException() {
        String longa = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, longa, null,
                        TipoEmprestimo.CONCEDIDO, valor("1.00"), DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComNomeTerceiroAcimaDe100LancaIllegalArgumentException() {
        String longa = "b".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", longa,
                        TipoEmprestimo.CONCEDIDO, valor("1.00"), DATA, false))
                .withMessageContaining("nomeTerceiro");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", null,
                        null, valor("1.00"), DATA, false))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComValorZeroLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", null,
                        TipoEmprestimo.CONCEDIDO, valor("0.00"), DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComValorNegativoLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", null,
                        TipoEmprestimo.CONCEDIDO, valor("-5.00"), DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComDataNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", null,
                        TipoEmprestimo.CONCEDIDO, valor("1.00"), null, false))
                .withMessageContaining("dataEmprestimo");
    }

    @Test
    void atualizarMudaCamposEAtualizadoEm() {
        Emprestimo e = new Emprestimo(USER_ID, "Antiga", "Joao",
                TipoEmprestimo.CONCEDIDO, valor("100.00"), DATA, false);
        Instant antes = e.getAtualizadoEm();
        LocalDate novaData = LocalDate.of(2026, 6, 30);

        e.atualizar("Nova descricao", "Maria", TipoEmprestimo.RECEBIDO,
                valor("250.00"), novaData, true);

        assertThat(e.getDescricao()).isEqualTo("Nova descricao");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(e.getValor().valor()).isEqualByComparingTo("250.00");
        assertThat(e.getDataEmprestimo()).isEqualTo(novaData);
        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void atualizarComValorInvalidoLancaIllegalArgumentException() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", null,
                TipoEmprestimo.CONCEDIDO, valor("100.00"), DATA, false);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> e.atualizar("Desc", null, TipoEmprestimo.CONCEDIDO,
                        valor("0.00"), DATA, false))
                .withMessageContaining("valor");
    }
}
