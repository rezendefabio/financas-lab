package com.laboratorio.financas.tag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TagTest {

    private static final UUID USER_ID = UUID.randomUUID();

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaTag() {
        Instant antes = Instant.now();

        Tag tag = new Tag(USER_ID, "Essencial", "#FF0000");

        Instant depois = Instant.now();
        assertThat(tag.getId()).isNotNull();
        assertThat(tag.getUserId()).isEqualTo(USER_ID);
        assertThat(tag.getNome()).isEqualTo("Essencial");
        assertThat(tag.getCor()).isEqualTo("#FF0000");
        assertThat(tag.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorNovoComCorNulaAceita() {
        Tag tag = new Tag(USER_ID, "Sem cor", null);
        assertThat(tag.getCor()).isNull();
    }

    @Test
    void construtorNovoComNomeComEspacosNasPontasTrimaONome() {
        Tag tag = new Tag(USER_ID, "  Lazer  ", null);
        assertThat(tag.getNome()).isEqualTo("Lazer");
    }

    @Test
    void construtorNovoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Tag(null, "Essencial", null))
                .withMessageContaining("userId");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Tag(USER_ID, null, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Tag(USER_ID, "   ", null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(51);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Tag(USER_ID, nomeLongo, null))
                .withMessageContaining("50");
    }

    @Test
    void construtorNovoComNomeExatamente50CharsAceita() {
        String nome50 = "a".repeat(50);
        Tag tag = new Tag(USER_ID, nome50, null);
        assertThat(tag.getNome()).hasSize(50);
    }

    @Test
    void duasTagsNovasTenIdsDiferentes() {
        Tag t1 = new Tag(USER_ID, "Tag A", null);
        Tag t2 = new Tag(USER_ID, "Tag B", null);
        assertThat(t1.getId()).isNotEqualTo(t2.getId());
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoComTodosOsCamposValidosPreservaValores() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");

        Tag tag = new Tag(id, USER_ID, "Lazer", "#00FF00", criadoEm);

        assertThat(tag.getId()).isEqualTo(id);
        assertThat(tag.getUserId()).isEqualTo(USER_ID);
        assertThat(tag.getNome()).isEqualTo("Lazer");
        assertThat(tag.getCor()).isEqualTo("#00FF00");
        assertThat(tag.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Tag(null, USER_ID, "Tag", null, Instant.now()))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Tag(UUID.randomUUID(), USER_ID, "Tag", null, null))
                .withMessageContaining("criadoEm");
    }

    // --- equals e hashCode ---

    @Test
    void duasTagsComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Tag t1 = new Tag(id, USER_ID, "Nome A", null, t);
        Tag t2 = new Tag(id, USER_ID, "Nome B", "#000", t);
        assertThat(t1).isEqualTo(t2);
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        Tag tag = new Tag(USER_ID, "Essencial", null);
        assertThat(tag.equals(null)).isFalse();
        assertThat(tag.equals("string")).isFalse();
    }

    @Test
    void hashCodeBasadoNoId() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Tag t1 = new Tag(id, USER_ID, "A", null, t);
        Tag t2 = new Tag(id, USER_ID, "B", "#FFF", t);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    // --- toString ---

    @Test
    void toStringContemIdUserIdENome() {
        Tag tag = new Tag(USER_ID, "Essencial", null);
        String resultado = tag.toString();
        assertThat(resultado)
                .contains(tag.getId().toString())
                .contains(USER_ID.toString())
                .contains("Essencial");
    }
}
