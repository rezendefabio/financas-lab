package com.laboratorio.financas.grupo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GrupoTest {

    private static final UUID USER_ID = UUID.randomUUID();

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaGrupo() {
        Instant antes = Instant.now();

        Grupo grupo = new Grupo(USER_ID, "Viagem Europa", "Gastos da viagem");

        Instant depois = Instant.now();
        assertThat(grupo.getId()).isNotNull();
        assertThat(grupo.getUserId()).isEqualTo(USER_ID);
        assertThat(grupo.getNome()).isEqualTo("Viagem Europa");
        assertThat(grupo.getDescricao()).isEqualTo("Gastos da viagem");
        assertThat(grupo.isAtivo()).isTrue();
        assertThat(grupo.getCriadoEm()).isBetween(antes, depois);
        assertThat(grupo.getAtualizadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorNovoComDescricaoNulaAceita() {
        Grupo grupo = new Grupo(USER_ID, "Casa Nova", null);
        assertThat(grupo.getDescricao()).isNull();
    }

    @Test
    void construtorNovoComNomeComEspacosNasPontasTrimaONome() {
        Grupo grupo = new Grupo(USER_ID, "  Lazer  ", null);
        assertThat(grupo.getNome()).isEqualTo("Lazer");
    }

    @Test
    void construtorNovoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Grupo(null, "Viagem", null))
                .withMessageContaining("userId");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Grupo(USER_ID, null, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Grupo(USER_ID, "   ", null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Grupo(USER_ID, nomeLongo, null))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        Grupo grupo = new Grupo(USER_ID, nome100, null);
        assertThat(grupo.getNome()).hasSize(100);
    }

    @Test
    void construtorNovoComDescricaoAcimaMaximoLancaIllegalArgumentException() {
        String descricaoLonga = "a".repeat(301);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Grupo(USER_ID, "Viagem", descricaoLonga))
                .withMessageContaining("300");
    }

    @Test
    void construtorNovoComDescricaoExatamente300CharsAceita() {
        String descricao300 = "a".repeat(300);
        Grupo grupo = new Grupo(USER_ID, "Viagem", descricao300);
        assertThat(grupo.getDescricao()).hasSize(300);
    }

    @Test
    void doisGruposNovosTemIdsDiferentes() {
        Grupo g1 = new Grupo(USER_ID, "Grupo A", null);
        Grupo g2 = new Grupo(USER_ID, "Grupo B", null);
        assertThat(g1.getId()).isNotEqualTo(g2.getId());
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoComTodosOsCamposValidosPreservaValores() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-02-01T10:00:00Z");

        Grupo grupo = new Grupo(id, USER_ID, "Lazer", "Descricao", false, criadoEm, atualizadoEm);

        assertThat(grupo.getId()).isEqualTo(id);
        assertThat(grupo.getUserId()).isEqualTo(USER_ID);
        assertThat(grupo.getNome()).isEqualTo("Lazer");
        assertThat(grupo.getDescricao()).isEqualTo("Descricao");
        assertThat(grupo.isAtivo()).isFalse();
        assertThat(grupo.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(grupo.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Grupo(null, USER_ID, "Grupo", null, true, Instant.now(), Instant.now()))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Grupo(UUID.randomUUID(), USER_ID, "Grupo", null, true, null, Instant.now()))
                .withMessageContaining("criadoEm");
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Grupo(UUID.randomUUID(), USER_ID, "Grupo", null, true, Instant.now(), null))
                .withMessageContaining("atualizadoEm");
    }

    // --- atualizar ---

    @Test
    void atualizarRetornaNovaInstanciaComNovosValores() {
        Grupo original = new Grupo(USER_ID, "Antigo", "Desc antiga");

        Grupo atualizado = original.atualizar("Novo", "Desc nova");

        assertThat(atualizado).isNotSameAs(original);
        assertThat(atualizado.getNome()).isEqualTo("Novo");
        assertThat(atualizado.getDescricao()).isEqualTo("Desc nova");
        assertThat(atualizado.getId()).isEqualTo(original.getId());
        assertThat(atualizado.getUserId()).isEqualTo(original.getUserId());
        assertThat(atualizado.isAtivo()).isEqualTo(original.isAtivo());
        assertThat(atualizado.getCriadoEm()).isEqualTo(original.getCriadoEm());
    }

    @Test
    void atualizarAtualizaTimestampAtualizadoEm() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Grupo original = new Grupo(id, USER_ID, "Antigo", null, true, criadoEm, atualizadoEm);

        Grupo atualizado = original.atualizar("Novo", null);

        assertThat(atualizado.getAtualizadoEm()).isAfter(atualizadoEm);
    }

    // --- desativar ---

    @Test
    void desativarRetornaNovaInstanciaComAtivoFalse() {
        Grupo original = new Grupo(USER_ID, "Ativo", null);

        Grupo desativado = original.desativar();

        assertThat(desativado).isNotSameAs(original);
        assertThat(desativado.isAtivo()).isFalse();
        assertThat(desativado.getId()).isEqualTo(original.getId());
        assertThat(desativado.getNome()).isEqualTo(original.getNome());
        assertThat(desativado.getCriadoEm()).isEqualTo(original.getCriadoEm());
    }

    @Test
    void desativarAtualizaTimestampAtualizadoEm() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Grupo original = new Grupo(id, USER_ID, "Ativo", null, true, criadoEm, atualizadoEm);

        Grupo desativado = original.desativar();

        assertThat(desativado.getAtualizadoEm()).isAfter(atualizadoEm);
    }

    // --- equals e hashCode ---

    @Test
    void doisGruposComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Grupo g1 = new Grupo(id, USER_ID, "Nome A", null, true, t, t);
        Grupo g2 = new Grupo(id, USER_ID, "Nome B", "Desc", false, t, t);
        assertThat(g1).isEqualTo(g2);
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        Grupo grupo = new Grupo(USER_ID, "Viagem", null);
        assertThat(grupo.equals(null)).isFalse();
        assertThat(grupo.equals("string")).isFalse();
    }

    @Test
    void hashCodeBaseadoNoId() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Grupo g1 = new Grupo(id, USER_ID, "A", null, true, t, t);
        Grupo g2 = new Grupo(id, USER_ID, "B", "X", false, t, t);
        assertThat(g1.hashCode()).isEqualTo(g2.hashCode());
    }

    // --- toString ---

    @Test
    void toStringContemIdUserIdENome() {
        Grupo grupo = new Grupo(USER_ID, "Viagem", null);
        String resultado = grupo.toString();
        assertThat(resultado)
                .contains(grupo.getId().toString())
                .contains(USER_ID.toString())
                .contains("Viagem");
    }
}
