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
    private static final Money VALOR = new Money(new BigDecimal("100.00"), Currency.getInstance("BRL"));
    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

    @Test
    void construtorCriacaoComArgumentosValidosCriaEmprestimo() {
        Instant antes = Instant.now();
        Emprestimo e = new Emprestimo(USER_ID, "Emprestimo X", "Joao",
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestimo X");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.getValor()).isEqualTo(VALOR);
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA);
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComNomeTerceiroNuloAceita() {
        Emprestimo e = new Emprestimo(USER_ID, "X", null,
                TipoEmprestimo.RECEBIDO, VALOR, DATA, false);
        assertThat(e.getNomeTerceiro()).isNull();
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(null, "X", null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComDescricaoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, null, null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "   ", null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoLongaLancaIllegalArgumentException() {
        String longa = "x".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, longa, null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        null, VALOR, DATA, false))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComValorZeroLancaIllegalArgumentException() {
        Money zero = new Money(BigDecimal.ZERO, Currency.getInstance("BRL"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        TipoEmprestimo.CONCEDIDO, zero, DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComValorNegativoLancaIllegalArgumentException() {
        Money neg = new Money(new BigDecimal("-1.00"), Currency.getInstance("BRL"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        TipoEmprestimo.CONCEDIDO, neg, DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComDataNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        TipoEmprestimo.CONCEDIDO, VALOR, null, false))
                .withMessageContaining("dataEmprestimo");
    }

    @Test
    void atualizarMudaCamposEAtualizaTimestamp() {
        Emprestimo e = new Emprestimo(USER_ID, "X", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        Instant antes = e.getAtualizadoEm();

        Money novo = new Money(new BigDecimal("200.00"), Currency.getInstance("BRL"));
        e.atualizar("Y", "Maria", TipoEmprestimo.RECEBIDO, novo,
                DATA.plusDays(1), true);

        assertThat(e.getDescricao()).isEqualTo("Y");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(e.getValor()).isEqualTo(novo);
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA.plusDays(1));
        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void equalsEHashCodeBaseadosNoId() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Emprestimo a = new Emprestimo(id, USER_ID, "A", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false, now, now);
        Emprestimo b = new Emprestimo(id, USER_ID, "B", "Outro",
                TipoEmprestimo.RECEBIDO, VALOR, DATA, true, now, now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
