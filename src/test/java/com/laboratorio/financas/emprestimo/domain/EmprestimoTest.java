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
    private static final Money VALOR = new Money(new BigDecimal("100.00"), BRL);
    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

    @Test
    void construtorCriacaoComArgumentosValidosCriaEmprestimo() {
        Instant antes = Instant.now();
        Emprestimo e = new Emprestimo(USER_ID, "Emprestimo Joao", "Joao",
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestimo Joao");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.getValor()).isEqualTo(VALOR);
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA);
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorAceitaNomeTerceiroNulo() {
        Emprestimo e = new Emprestimo(USER_ID, "Sem terceiro", null,
                TipoEmprestimo.RECEBIDO, VALOR, DATA, true);
        assertThat(e.getNomeTerceiro()).isNull();
        assertThat(e.isQuitado()).isTrue();
    }

    @Test
    void construtorTransformaNomeTerceiroBlankEmNull() {
        Emprestimo e = new Emprestimo(USER_ID, "X", "   ",
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        assertThat(e.getNomeTerceiro()).isNull();
    }

    @Test
    void construtorComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(null, "X", null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("userId");
    }

    @Test
    void construtorComDescricaoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, null, null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorComDescricaoBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "   ", null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorComDescricaoMuitoLongaLancaIllegalArgumentException() {
        String longa = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, longa, null,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorComNomeTerceiroMuitoLongoLancaIllegalArgumentException() {
        String longo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", longo,
                        TipoEmprestimo.CONCEDIDO, VALOR, DATA, false))
                .withMessageContaining("nomeTerceiro");
    }

    @Test
    void construtorComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        null, VALOR, DATA, false))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorComValorNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        TipoEmprestimo.CONCEDIDO, null, DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void construtorComValorZeroLancaIllegalArgumentException() {
        Money zero = new Money(BigDecimal.ZERO, BRL);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        TipoEmprestimo.CONCEDIDO, zero, DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void construtorComValorNegativoLancaIllegalArgumentException() {
        Money negativo = new Money(new BigDecimal("-1.00"), BRL);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        TipoEmprestimo.CONCEDIDO, negativo, DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void construtorComDataNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "X", null,
                        TipoEmprestimo.CONCEDIDO, VALOR, null, false))
                .withMessageContaining("dataEmprestimo");
    }

    @Test
    void atualizarMudaCamposEAtualizaTimestamp() throws InterruptedException {
        Emprestimo e = new Emprestimo(USER_ID, "Antiga", "Joao",
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        Instant antes = e.getAtualizadoEm();
        Thread.sleep(2);
        Money novoValor = new Money(new BigDecimal("200.00"), BRL);
        LocalDate novaData = LocalDate.of(2026, 2, 1);

        e.atualizar("Nova", "Maria", TipoEmprestimo.RECEBIDO, novoValor, novaData, true);

        assertThat(e.getDescricao()).isEqualTo("Nova");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(e.getValor()).isEqualTo(novoValor);
        assertThat(e.getDataEmprestimo()).isEqualTo(novaData);
        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfter(antes);
    }

    @Test
    void atualizarComValorInvalidoLancaExcecao() {
        Emprestimo e = new Emprestimo(USER_ID, "X", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        Money zero = new Money(BigDecimal.ZERO, BRL);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> e.atualizar("X", null, TipoEmprestimo.CONCEDIDO, zero, DATA, false))
                .withMessageContaining("valor");
    }

    @Test
    void equalsBaseadoEmId() {
        UUID id = UUID.randomUUID();
        Instant agora = Instant.now();
        Emprestimo a = new Emprestimo(id, USER_ID, "X", null, TipoEmprestimo.CONCEDIDO,
                VALOR, DATA, false, agora, agora);
        Emprestimo b = new Emprestimo(id, USER_ID, "Y", null, TipoEmprestimo.RECEBIDO,
                VALOR, DATA, true, agora, agora);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
