package com.laboratorio.financas.lembrete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LembreteTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void construtorCriacaoComArgumentosValidosCriaLembrete() {
        Lembrete lembrete = new Lembrete(
                USER_ID, "Pagar conta", "Boleto agua",
                LocalDate.of(2026, 6, 15), PrioridadeLembrete.MEDIA, false);

        assertThat(lembrete.getId()).isNotNull();
        assertThat(lembrete.getUserId()).isEqualTo(USER_ID);
        assertThat(lembrete.getTitulo()).isEqualTo("Pagar conta");
        assertThat(lembrete.getDescricao()).isEqualTo("Boleto agua");
        assertThat(lembrete.getDataLembrete()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(lembrete.getPrioridade()).isEqualTo(PrioridadeLembrete.MEDIA);
        assertThat(lembrete.isConcluido()).isFalse();
        assertThat(lembrete.getCriadoEm()).isNotNull();
        assertThat(lembrete.getAtualizadoEm()).isNull();
    }

    @Test
    void construtorAceitaDescricaoNula() {
        Lembrete lembrete = new Lembrete(
                USER_ID, "Lembrete", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);
        assertThat(lembrete.getDescricao()).isNull();
    }

    @Test
    void construtorComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(
                        null, "Titulo", null, LocalDate.now(), PrioridadeLembrete.BAIXA, false))
                .withMessageContaining("userId");
    }

    @Test
    void construtorComTituloNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(
                        USER_ID, null, null, LocalDate.now(), PrioridadeLembrete.BAIXA, false))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorComTituloBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Lembrete(
                        USER_ID, "   ", null, LocalDate.now(), PrioridadeLembrete.BAIXA, false))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorComTituloAcimaDoLimiteLancaIllegalArgumentException() {
        String tituloGrande = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Lembrete(
                        USER_ID, tituloGrande, null, LocalDate.now(), PrioridadeLembrete.BAIXA, false))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorComDescricaoAcimaDoLimiteLancaIllegalArgumentException() {
        String descGrande = "a".repeat(501);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Lembrete(
                        USER_ID, "Titulo", descGrande, LocalDate.now(), PrioridadeLembrete.BAIXA, false))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorComDataNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(
                        USER_ID, "Titulo", null, null, PrioridadeLembrete.BAIXA, false))
                .withMessageContaining("dataLembrete");
    }

    @Test
    void construtorComPrioridadeNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(
                        USER_ID, "Titulo", null, LocalDate.now(), null, false))
                .withMessageContaining("prioridade");
    }

    @Test
    void atualizarModificaCamposEAtualizaTimestamp() {
        Lembrete lembrete = new Lembrete(
                USER_ID, "Antigo", "desc", LocalDate.now(), PrioridadeLembrete.BAIXA, false);

        lembrete.atualizar("Novo", "nova desc",
                LocalDate.of(2026, 7, 1), PrioridadeLembrete.ALTA, true);

        assertThat(lembrete.getTitulo()).isEqualTo("Novo");
        assertThat(lembrete.getDescricao()).isEqualTo("nova desc");
        assertThat(lembrete.getDataLembrete()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(lembrete.getPrioridade()).isEqualTo(PrioridadeLembrete.ALTA);
        assertThat(lembrete.isConcluido()).isTrue();
        assertThat(lembrete.getAtualizadoEm()).isNotNull();
    }

    @Test
    void equalsBaseadoEmId() {
        UUID id = UUID.randomUUID();
        Lembrete a = new Lembrete(id, USER_ID, "A", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false,
                java.time.Instant.now(), null);
        Lembrete b = new Lembrete(id, USER_ID, "B", "x",
                LocalDate.now(), PrioridadeLembrete.ALTA, true,
                java.time.Instant.now(), java.time.Instant.now());
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
