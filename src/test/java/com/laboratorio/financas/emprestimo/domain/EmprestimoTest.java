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
    private static final LocalDate DATA = LocalDate.of(2026, 5, 30);

    private static Money valor(String v) {
        return new Money(new BigDecimal(v), BRL);
    }

    @Test
    void construtorCriacaoComArgumentosValidosCriaEmprestimo() {
        Instant antes = Instant.now();
        Emprestimo e = new Emprestimo(USER_ID, "Emprestei ao Joao", "Joao",
                TipoEmprestimo.CONCEDIDO, valor("100.00"), DATA);
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestei ao Joao");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.getValor().valor()).isEqualByComparingTo("100.00");
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA);
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComNomeTerceiroNuloEhPermitido() {
        Emprestimo e = new Emprestimo(USER_ID, "Sem terceiro", null,
                TipoEmprestimo.RECEBIDO, valor("50.00"), DATA);

        assertThat(e.getNomeTerceiro()).isNull();
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(null, "Desc", "T",
                        TipoEmprestimo.CONCEDIDO, valor("10.00"), DATA))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComDescricaoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, null, "T",
                        TipoEmprestimo.CONCEDIDO, valor("10.00"), DATA))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "  ", "T",
                        TipoEmprestimo.CONCEDIDO, valor("10.00"), DATA))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "T",
                        null, valor("10.00"), DATA))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComValorNaoPositivoLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "T",
                        TipoEmprestimo.CONCEDIDO, valor("0.00"), DATA))
                .withMessageContaining("valor");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "T",
                        TipoEmprestimo.CONCEDIDO, valor("-5.00"), DATA))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComDataNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "T",
                        TipoEmprestimo.CONCEDIDO, valor("10.00"), null))
                .withMessageContaining("dataEmprestimo");
    }

    @Test
    void atualizarMudaCamposEAtualizaTimestamp() {
        Emprestimo e = new Emprestimo(USER_ID, "Antigo", "Joao",
                TipoEmprestimo.CONCEDIDO, valor("100.00"), DATA);
        Instant antes = e.getAtualizadoEm();

        e.atualizar("Novo", "Maria", TipoEmprestimo.RECEBIDO,
                valor("200.00"), DATA.plusDays(1), true);

        assertThat(e.getDescricao()).isEqualTo("Novo");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(e.getValor().valor()).isEqualByComparingTo("200.00");
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA.plusDays(1));
        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void atualizarComValorNaoPositivoLancaIllegalArgumentException() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", "T",
                TipoEmprestimo.CONCEDIDO, valor("100.00"), DATA);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> e.atualizar("Desc", "T", TipoEmprestimo.CONCEDIDO,
                        valor("-1.00"), DATA, false))
                .withMessageContaining("valor");
    }
}
