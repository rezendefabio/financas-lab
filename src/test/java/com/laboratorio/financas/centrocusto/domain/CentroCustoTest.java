package com.laboratorio.financas.centrocusto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CentroCustoTest {

    private static final UUID USER_ID = UUID.randomUUID();

    // --- Construtor de criacao ---

    @Test
    void construtorNovoComArgumentosValidosCriaCentroCustoAtivo() {
        Instant antes = Instant.now();
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", "Despesas residenciais");
        Instant depois = Instant.now();

        assertThat(cc.getId()).isNotNull();
        assertThat(cc.getUserId()).isEqualTo(USER_ID);
        assertThat(cc.getNome()).isEqualTo("Casa");
        assertThat(cc.getDescricao()).isEqualTo("Despesas residenciais");
        assertThat(cc.isAtivo()).isTrue();
        assertThat(cc.getCriadoEm()).isBetween(antes, depois);
        assertThat(cc.getAtualizadoEm()).isEqualTo(cc.getCriadoEm());
    }

    @Test
    void construtorNovoComDescricaoNulaPreservaNulo() {
        CentroCusto cc = new CentroCusto(USER_ID, "Trabalho", null);
        assertThat(cc.getDescricao()).isNull();
    }

    @Test
    void construtorNovoTrimaNomeEDescricao() {
        CentroCusto cc = new CentroCusto(USER_ID, "  Casa  ", "  Despesas  ");
        assertThat(cc.getNome()).isEqualTo("Casa");
        assertThat(cc.getDescricao()).isEqualTo("Despesas");
    }

    @Test
    void construtorNovoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CentroCusto(null, "Casa", null))
                .withMessageContaining("userId");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CentroCusto(USER_ID, null, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CentroCusto(USER_ID, "   ", null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CentroCusto(USER_ID, nomeLongo, null))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        CentroCusto cc = new CentroCusto(USER_ID, nome100, null);
        assertThat(cc.getNome()).hasSize(100);
    }

    @Test
    void construtorNovoComDescricaoAcimaMaximoLancaIllegalArgumentException() {
        String descLonga = "a".repeat(256);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CentroCusto(USER_ID, "Casa", descLonga))
                .withMessageContaining("255");
    }

    @Test
    void construtorNovoComDescricaoExatamente255CharsAceita() {
        String desc255 = "a".repeat(255);
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", desc255);
        assertThat(cc.getDescricao()).hasSize(255);
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoPreservaTodosCampos() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-01-02T12:00:00Z");

        CentroCusto cc = new CentroCusto(id, USER_ID, "Casa", "Descricao", false, criadoEm, atualizadoEm);

        assertThat(cc.getId()).isEqualTo(id);
        assertThat(cc.getUserId()).isEqualTo(USER_ID);
        assertThat(cc.getNome()).isEqualTo("Casa");
        assertThat(cc.getDescricao()).isEqualTo("Descricao");
        assertThat(cc.isAtivo()).isFalse();
        assertThat(cc.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(cc.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CentroCusto(null, USER_ID, "Casa", null, true, Instant.now(), null))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CentroCusto(UUID.randomUUID(), USER_ID, "Casa", null, true, null, null))
                .withMessageContaining("criadoEm");
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmNuloDefaultaParaCriadoEm() {
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        CentroCusto cc = new CentroCusto(UUID.randomUUID(), USER_ID, "Casa", null, true, criadoEm, null);
        assertThat(cc.getAtualizadoEm()).isEqualTo(criadoEm);
    }

    // --- desativar() ---

    @Test
    void desativarRetornaNovaInstanciaComAtivoFalse() {
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", null);
        CentroCusto desativado = cc.desativar();
        assertThat(desativado.isAtivo()).isFalse();
        assertThat(cc.isAtivo()).isTrue(); // original nao mudou
    }

    @Test
    void desativarPreservaIdUserIdNomeDescricaoCriadoEm() {
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", "desc");
        CentroCusto desativado = cc.desativar();
        assertThat(desativado.getId()).isEqualTo(cc.getId());
        assertThat(desativado.getUserId()).isEqualTo(cc.getUserId());
        assertThat(desativado.getNome()).isEqualTo(cc.getNome());
        assertThat(desativado.getDescricao()).isEqualTo(cc.getDescricao());
        assertThat(desativado.getCriadoEm()).isEqualTo(cc.getCriadoEm());
    }

    @Test
    void desativarJaInativoRetornaMesmaInstancia() {
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", null);
        CentroCusto desativado = cc.desativar();
        CentroCusto novamente = desativado.desativar();
        assertThat(novamente).isSameAs(desativado);
    }

    // --- equals e hashCode ---

    @Test
    void doisCentroCustosComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        CentroCusto c1 = new CentroCusto(id, USER_ID, "Casa", null, true, t, t);
        CentroCusto c2 = new CentroCusto(id, UUID.randomUUID(), "Trabalho", null, false, t, t);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", null);
        assertThat(cc.equals(null)).isFalse();
        assertThat(cc.equals("string")).isFalse();
    }

    @Test
    void toStringContemIdNomeEAtivo() {
        CentroCusto cc = new CentroCusto(USER_ID, "Casa", null);
        String resultado = cc.toString();
        assertThat(resultado)
                .contains(cc.getId().toString())
                .contains("Casa")
                .contains("ativo");
    }
}
