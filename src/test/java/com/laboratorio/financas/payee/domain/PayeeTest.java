package com.laboratorio.financas.payee.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayeeTest {

    private static final UUID USER_ID = UUID.randomUUID();

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaPayee() {
        Instant antes = Instant.now();
        Payee payee = new Payee(USER_ID, "Mercado Extra", null);
        Instant depois = Instant.now();

        assertThat(payee.getId()).isNotNull();
        assertThat(payee.getUserId()).isEqualTo(USER_ID);
        assertThat(payee.getNome()).isEqualTo("Mercado Extra");
        assertThat(payee.getCategoriaPadraoId()).isNull();
        assertThat(payee.getCriadoEm()).isBetween(antes, depois);
        assertThat(payee.getAtualizadoEm()).isEqualTo(payee.getCriadoEm());
    }

    @Test
    void construtorNovoComCategoriaPadraoIdPreservaCampo() {
        UUID categoriaId = UUID.randomUUID();
        Payee payee = new Payee(USER_ID, "Padaria", categoriaId);
        assertThat(payee.getCategoriaPadraoId()).isEqualTo(categoriaId);
    }

    @Test
    void construtorNovoComNomeComEspacosNasPontasTrimaONome() {
        Payee payee = new Payee(USER_ID, "  Supermercado  ", null);
        assertThat(payee.getNome()).isEqualTo("Supermercado");
    }

    @Test
    void construtorNovoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Payee(null, "Padaria", null))
                .withMessageContaining("userId");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Payee(USER_ID, null, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Payee(USER_ID, "   ", null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Payee(USER_ID, nomeLongo, null))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        Payee payee = new Payee(USER_ID, nome100, null);
        assertThat(payee.getNome()).hasSize(100);
    }

    @Test
    void doisPayeesNovosTenIdsDiferentes() {
        Payee p1 = new Payee(USER_ID, "Farmacia", null);
        Payee p2 = new Payee(USER_ID, "Academia", null);
        assertThat(p1.getId()).isNotEqualTo(p2.getId());
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoComTodosOsCamposValidosPreservaValores() {
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-01-02T12:00:00Z");

        Payee payee = new Payee(id, USER_ID, "Padaria", categoriaId, criadoEm, atualizadoEm);

        assertThat(payee.getId()).isEqualTo(id);
        assertThat(payee.getUserId()).isEqualTo(USER_ID);
        assertThat(payee.getNome()).isEqualTo("Padaria");
        assertThat(payee.getCategoriaPadraoId()).isEqualTo(categoriaId);
        assertThat(payee.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(payee.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Payee(null, USER_ID, "Padaria", null, Instant.now(), null))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Payee(UUID.randomUUID(), null, "Padaria", null, Instant.now(), null))
                .withMessageContaining("userId");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Payee(UUID.randomUUID(), USER_ID, "Padaria", null, null, null))
                .withMessageContaining("criadoEm");
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmNuloDefaultaParaCriadoEm() {
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Payee payee = new Payee(UUID.randomUUID(), USER_ID, "Farmacia", null, criadoEm, null);
        assertThat(payee.getAtualizadoEm()).isEqualTo(criadoEm);
    }

    // --- equals e hashCode ---

    @Test
    void doisPayeesComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Payee p1 = new Payee(id, USER_ID, "Padaria", null, t, t);
        Payee p2 = new Payee(id, UUID.randomUUID(), "Farmacia", null, t, t);
        assertThat(p1).isEqualTo(p2);
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        Payee payee = new Payee(USER_ID, "Padaria", null);
        assertThat(payee.equals(null)).isFalse();
        assertThat(payee.equals("string")).isFalse();
    }

    // --- toString ---

    @Test
    void toStringContemIdUserIdENome() {
        Payee payee = new Payee(USER_ID, "Supermercado", null);
        String resultado = payee.toString();
        assertThat(resultado)
                .contains(payee.getId().toString())
                .contains(USER_ID.toString())
                .contains("Supermercado");
    }

    @Test
    void toStringNaoContemTimestamps() {
        Payee payee = new Payee(USER_ID, "Supermercado", null);
        String resultado = payee.toString();
        assertThat(resultado)
                .doesNotContain("criadoEm")
                .doesNotContain("atualizadoEm");
    }
}
