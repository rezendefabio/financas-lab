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

    private static Money valorValido() {
        return new Money(new BigDecimal("100.00"), BRL);
    }

    @Test
    void construtorCriacaoComArgumentosValidosCriaEmprestimo() {
        Instant antes = Instant.now();
        Emprestimo e = new Emprestimo(USER_ID, "Emprestimo ao Joao", "Joao",
                TipoEmprestimo.CONCEDIDO, valorValido(), DATA);
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getDescricao()).isEqualTo("Emprestimo ao Joao");
        assertThat(e.getNomeTerceiro()).isEqualTo("Joao");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(e.getValor()).isEqualTo(valorValido());
        assertThat(e.getDataEmprestimo()).isEqualTo(DATA);
        assertThat(e.isQuitado()).isFalse();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "Joao", null, valorValido(), DATA))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoComDescricaoBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "  ", "Joao",
                        TipoEmprestimo.CONCEDIDO, valorValido(), DATA))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComValorNaoPositivoLancaIllegalArgumentException() {
        Money zero = new Money(BigDecimal.ZERO, BRL);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Emprestimo(USER_ID, "Desc", "Joao",
                        TipoEmprestimo.CONCEDIDO, zero, DATA))
                .withMessageContaining("valor");
    }

    @Test
    void construtorCriacaoIniciaQuitadoComoFalse() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", null,
                TipoEmprestimo.RECEBIDO, valorValido(), DATA);
        assertThat(e.isQuitado()).isFalse();
    }

    @Test
    void construtorCriacaoAceitaNomeTerceiroNulo() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", null,
                TipoEmprestimo.RECEBIDO, valorValido(), DATA);
        assertThat(e.getNomeTerceiro()).isNull();
    }

    @Test
    void atualizarAlteraCamposEAtualizaTimestamp() {
        Emprestimo e = new Emprestimo(USER_ID, "Desc", "Joao",
                TipoEmprestimo.CONCEDIDO, valorValido(), DATA);
        Instant antes = e.getAtualizadoEm();
        Money novoValor = new Money(new BigDecimal("250.00"), BRL);
        LocalDate novaData = LocalDate.of(2026, 6, 1);

        e.atualizar("Nova Desc", "Maria", TipoEmprestimo.RECEBIDO, novoValor, novaData, true);

        assertThat(e.getDescricao()).isEqualTo("Nova Desc");
        assertThat(e.getNomeTerceiro()).isEqualTo("Maria");
        assertThat(e.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(e.getValor()).isEqualTo(novoValor);
        assertThat(e.getDataEmprestimo()).isEqualTo(novaData);
        assertThat(e.isQuitado()).isTrue();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }
}
