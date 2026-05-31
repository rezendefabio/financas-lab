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

    private static Money money(String valor) {
        return new Money(new BigDecimal(valor), BRL);
    }

    @Test
    void construtorCriacaoComArgumentosValidosCriaEmprestimo() {
        Instant antes = Instant.now();
        Emprestimo e = new Emprestimo(USER_ID, "Emprestimo ao Joao", "Joao",
                TipoEmprestimo.CONCEDIDO, money("100.00"), DATA);
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestimo ao Joao");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.getValor()).isEqualTo(money("100.00"));
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA);
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(null, "Desc", "Joao",
                        TipoEmprestimo.CONCEDIDO, money("100.00"), DATA))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComDescricaoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, null, "Joao",
                        TipoEmprestimo.CONCEDIDO, money("100.00"), DATA))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "   ", "Joao",
                        TipoEmprestimo.CONCEDIDO, money("100.00"), DATA))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDescricaoMaiorQue100LancaIllegalArgumentException() {
        String descricao = "x".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, descricao, "Joao",
                        TipoEmprestimo.CONCEDIDO, money("100.00"), DATA))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "Joao",
                        null, money("100.00"), DATA))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComValorNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "Joao",
                        TipoEmprestimo.CONCEDIDO, null, DATA))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComValorNaoPositivoLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "Joao",
                        TipoEmprestimo.CONCEDIDO, money("0.00"), DATA))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoComDataEmprestimoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "Joao",
                        TipoEmprestimo.CONCEDIDO, money("100.00"), null))
                .withMessageContaining("dataEmprestimo");
    }

    @Test
    void nomeTerceiroPodeSerNulo() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", null,
                TipoEmprestimo.RECEBIDO, money("50.00"), DATA);
        assertThat(e.getNomeTerceiro()).isNull();
    }

    @Test
    void atualizarAlteraCamposEAtualizaTimestamp() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", "Joao",
                TipoEmprestimo.CONCEDIDO, money("100.00"), DATA);
        Instant antes = e.getAtualizadoEm();

        e.atualizar("Nova Desc", "Maria", money("200.00"),
                LocalDate.of(2026, 2, 1), true);

        assertThat(e.getDescricao()).isEqualTo("Nova Desc");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getValor()).isEqualTo(money("200.00"));
        assertThat(e.getDataEmprestimo()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void marcarQuitadoMudaQuitadoParaTrueEAtualizaTimestamp() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", "Joao",
                TipoEmprestimo.CONCEDIDO, money("100.00"), DATA);
        Instant antes = e.getAtualizadoEm();

        e.marcarQuitado();

        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }
}
