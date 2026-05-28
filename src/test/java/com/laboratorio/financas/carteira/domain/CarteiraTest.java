package com.laboratorio.financas.carteira.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CarteiraTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTA_ID = UUID.randomUUID();

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaCarteira() {
        Instant antes = Instant.now();

        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "Tesouro", TipoCarteira.RENDA_FIXA);

        Instant depois = Instant.now();
        assertThat(carteira.getId()).isNotNull();
        assertThat(carteira.getUserId()).isEqualTo(USER_ID);
        assertThat(carteira.getContaId()).isEqualTo(CONTA_ID);
        assertThat(carteira.getNome()).isEqualTo("Tesouro");
        assertThat(carteira.getTipo()).isEqualTo(TipoCarteira.RENDA_FIXA);
        assertThat(carteira.isAtivo()).isTrue();
        assertThat(carteira.getCriadoEm()).isBetween(antes, depois);
        assertThat(carteira.getAtualizadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorNovoComNomeComEspacosNasPontasTrimaONome() {
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "  Cripto  ", TipoCarteira.CRIPTOMOEDA);
        assertThat(carteira.getNome()).isEqualTo("Cripto");
    }

    @Test
    void construtorNovoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Carteira(null, CONTA_ID, "Tesouro", TipoCarteira.RENDA_FIXA))
                .withMessageContaining("userId");
    }

    @Test
    void construtorNovoComContaIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Carteira(USER_ID, null, "Tesouro", TipoCarteira.RENDA_FIXA))
                .withMessageContaining("contaId");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Carteira(USER_ID, CONTA_ID, null, TipoCarteira.RENDA_FIXA))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Carteira(USER_ID, CONTA_ID, "   ", TipoCarteira.RENDA_FIXA))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Carteira(USER_ID, CONTA_ID, nomeLongo, TipoCarteira.RENDA_FIXA))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, nome100, TipoCarteira.OUTROS);
        assertThat(carteira.getNome()).hasSize(100);
    }

    @Test
    void construtorNovoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Carteira(USER_ID, CONTA_ID, "Tesouro", null))
                .withMessageContaining("tipo");
    }

    @Test
    void duasCarteirasNovasTemIdsDiferentes() {
        Carteira c1 = new Carteira(USER_ID, CONTA_ID, "A", TipoCarteira.RENDA_FIXA);
        Carteira c2 = new Carteira(USER_ID, CONTA_ID, "B", TipoCarteira.RENDA_VARIAVEL);
        assertThat(c1.getId()).isNotEqualTo(c2.getId());
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoComTodosOsCamposValidosPreservaValores() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-02-01T10:00:00Z");

        Carteira carteira = new Carteira(
                id, USER_ID, CONTA_ID, "Acoes", TipoCarteira.RENDA_VARIAVEL, false, criadoEm, atualizadoEm);

        assertThat(carteira.getId()).isEqualTo(id);
        assertThat(carteira.getUserId()).isEqualTo(USER_ID);
        assertThat(carteira.getContaId()).isEqualTo(CONTA_ID);
        assertThat(carteira.getNome()).isEqualTo("Acoes");
        assertThat(carteira.getTipo()).isEqualTo(TipoCarteira.RENDA_VARIAVEL);
        assertThat(carteira.isAtivo()).isFalse();
        assertThat(carteira.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(carteira.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Carteira(
                        null, USER_ID, CONTA_ID, "C", TipoCarteira.OUTROS, true, Instant.now(), Instant.now()))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Carteira(
                        UUID.randomUUID(), USER_ID, CONTA_ID, "C", TipoCarteira.OUTROS, true, null, Instant.now()))
                .withMessageContaining("criadoEm");
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Carteira(
                        UUID.randomUUID(), USER_ID, CONTA_ID, "C", TipoCarteira.OUTROS, true, Instant.now(), null))
                .withMessageContaining("atualizadoEm");
    }

    // --- desativar ---

    @Test
    void desativarMarcaComoInativoEAtualizaTimestamp() {
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "Tesouro", TipoCarteira.RENDA_FIXA);
        Instant antesDesativar = carteira.getAtualizadoEm();

        carteira.desativar();

        assertThat(carteira.isAtivo()).isFalse();
        assertThat(carteira.getAtualizadoEm()).isAfterOrEqualTo(antesDesativar);
    }

    // --- atualizar ---

    @Test
    void atualizarAlteraNomeETipoEAtualizaTimestamp() {
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "Antiga", TipoCarteira.RENDA_FIXA);
        Instant antesAtualizar = carteira.getAtualizadoEm();

        carteira.atualizar("Nova", TipoCarteira.CRIPTOMOEDA);

        assertThat(carteira.getNome()).isEqualTo("Nova");
        assertThat(carteira.getTipo()).isEqualTo(TipoCarteira.CRIPTOMOEDA);
        assertThat(carteira.getAtualizadoEm()).isAfterOrEqualTo(antesAtualizar);
    }

    @Test
    void atualizarComNomeBlankLancaIllegalArgumentException() {
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "Antiga", TipoCarteira.RENDA_FIXA);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> carteira.atualizar("   ", TipoCarteira.OUTROS))
                .withMessageContaining("nome");
    }

    @Test
    void atualizarComTipoNuloLancaNullPointerException() {
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "Antiga", TipoCarteira.RENDA_FIXA);
        assertThatNullPointerException()
                .isThrownBy(() -> carteira.atualizar("Nova", null))
                .withMessageContaining("tipo");
    }

    // --- equals e hashCode ---

    @Test
    void duasCarteirasComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Carteira c1 = new Carteira(id, USER_ID, CONTA_ID, "Nome A", TipoCarteira.RENDA_FIXA, true, t, t);
        Carteira c2 = new Carteira(id, USER_ID, CONTA_ID, "Nome B", TipoCarteira.OUTROS, false, t, t);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "Tesouro", TipoCarteira.RENDA_FIXA);
        assertThat(carteira.equals(null)).isFalse();
        assertThat(carteira.equals("string")).isFalse();
    }

    // --- toString ---

    @Test
    void toStringContemIdUserIdENome() {
        Carteira carteira = new Carteira(USER_ID, CONTA_ID, "Tesouro", TipoCarteira.RENDA_FIXA);
        String resultado = carteira.toString();
        assertThat(resultado)
                .contains(carteira.getId().toString())
                .contains(USER_ID.toString())
                .contains("Tesouro");
    }
}
