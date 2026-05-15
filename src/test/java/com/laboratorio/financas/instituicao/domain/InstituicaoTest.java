package com.laboratorio.financas.instituicao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstituicaoTest {

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaInstituicao() {
        // Given
        Instant antes = Instant.now();

        // When
        Instituicao inst = new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true);

        // Then
        Instant depois = Instant.now();
        assertThat(inst.getId()).isNotNull();
        assertThat(inst.getCriadoEm()).isBetween(antes, depois);
        assertThat(inst.getNome()).isEqualTo("Nubank");
        assertThat(inst.getCodigoBanco()).isEqualTo("260");
        assertThat(inst.getTipo()).isEqualTo(TipoInstituicao.BANCO_DIGITAL);
        assertThat(inst.getLogoUrl()).isNull();
        assertThat(inst.isAtiva()).isTrue();
    }

    @Test
    void construtorNovoComNomeComEspacosNasPontasTrimaONome() {
        Instituicao inst = new Instituicao("  Bradesco  ", null, TipoInstituicao.BANCO_TRADICIONAL, null, true);
        assertThat(inst.getNome()).isEqualTo("Bradesco");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Instituicao(null, null, TipoInstituicao.BANCO_DIGITAL, null, true))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Instituicao("   ", null, TipoInstituicao.BANCO_DIGITAL, null, true))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Instituicao(nomeLongo, null, TipoInstituicao.BANCO_DIGITAL, null, true))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        Instituicao inst = new Instituicao(nome100, null, TipoInstituicao.OUTRO, null, true);
        assertThat(inst.getNome()).hasSize(100);
    }

    @Test
    void construtorNovoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Instituicao("Nubank", "260", null, null, true))
                .withMessageContaining("tipo");
    }

    @Test
    void duasInstituicoesNovasTenIdsDiferentes() {
        Instituicao i1 = new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true);
        Instituicao i2 = new Instituicao("Inter", "077", TipoInstituicao.BANCO_DIGITAL, null, true);
        assertThat(i1.getId()).isNotEqualTo(i2.getId());
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoComTodosOsCamposValidosPreservaValores() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");

        Instituicao inst = new Instituicao(id, "Banco do Brasil", "001",
                TipoInstituicao.BANCO_TRADICIONAL, "https://example.com/logo.png", true, criadoEm);

        assertThat(inst.getId()).isEqualTo(id);
        assertThat(inst.getNome()).isEqualTo("Banco do Brasil");
        assertThat(inst.getCodigoBanco()).isEqualTo("001");
        assertThat(inst.getTipo()).isEqualTo(TipoInstituicao.BANCO_TRADICIONAL);
        assertThat(inst.getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(inst.isAtiva()).isTrue();
        assertThat(inst.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void construtorReconstrucaoComCamposNullaveisAceita() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.now();

        Instituicao inst = new Instituicao(id, "XP", null, TipoInstituicao.CORRETORA, null, true, criadoEm);

        assertThat(inst.getCodigoBanco()).isNull();
        assertThat(inst.getLogoUrl()).isNull();
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Instituicao(null, "Nubank", "260",
                        TipoInstituicao.BANCO_DIGITAL, null, true, Instant.now()))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Instituicao(UUID.randomUUID(), "Nubank", "260",
                        TipoInstituicao.BANCO_DIGITAL, null, true, null))
                .withMessageContaining("criadoEm");
    }

    // --- equals e hashCode ---

    @Test
    void duasInstituicoesComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Instituicao i1 = new Instituicao(id, "Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true, t);
        Instituicao i2 = new Instituicao(id, "Inter", "077", TipoInstituicao.BANCO_DIGITAL, null, false, t);
        assertThat(i1).isEqualTo(i2);
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        Instituicao inst = new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true);
        assertThat(inst.equals(null)).isFalse();
        assertThat(inst.equals("string")).isFalse();
    }

    // --- toString ---

    @Test
    void toStringContemIdNomeETipo() {
        Instituicao inst = new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true);
        String resultado = inst.toString();
        assertThat(resultado)
                .contains(inst.getId().toString())
                .contains("Nubank")
                .contains("BANCO_DIGITAL");
    }

    // --- isAtiva ---

    @Test
    void instituicaoInativaRetornaAtivaFalse() {
        Instituicao inst = new Instituicao("Teste", null, TipoInstituicao.OUTRO, null, false);
        assertThat(inst.isAtiva()).isFalse();
    }

    // --- Todos os tipos de TipoInstituicao ---

    @Test
    void todosOsTiposDeInstituicaoSaoAceitos() {
        for (TipoInstituicao tipo : TipoInstituicao.values()) {
            Instituicao inst = new Instituicao("Banco Teste", null, tipo, null, true);
            assertThat(inst.getTipo()).isEqualTo(tipo);
        }
    }
}
