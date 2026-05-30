package com.laboratorio.financas.lembrete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LembreteTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATA = LocalDate.of(2026, 6, 1);

    @Test
    void construtorCriacaoComArgumentosValidosCriaLembrete() {
        Instant antes = Instant.now();
        Lembrete l = new Lembrete(USER_ID, "Pagar conta", "Conta de luz",
                DATA, Prioridade.MEDIA);
        Instant depois = Instant.now();

        assertThat(l.getId()).isNotNull();
        assertThat(l.getUserId()).isEqualTo(USER_ID);
        assertThat(l.getTitulo()).isEqualTo("Pagar conta");
        assertThat(l.getDescricao()).isEqualTo("Conta de luz");
        assertThat(l.getDataLembrete()).isEqualTo(DATA);
        assertThat(l.getPrioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(l.isConcluido()).isFalse();
        assertThat(l.getCriadoEm()).isBetween(antes, depois);
        assertThat(l.getAtualizadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComDescricaoNulaCria() {
        Lembrete l = new Lembrete(USER_ID, "T", null, DATA, Prioridade.BAIXA);
        assertThat(l.getDescricao()).isNull();
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(null, "T", null, DATA, Prioridade.BAIXA))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComTituloNuloLancaNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(USER_ID, null, null, DATA, Prioridade.BAIXA))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorCriacaoComTituloBlankLancaIae() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Lembrete(USER_ID, "   ", null, DATA, Prioridade.BAIXA))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorCriacaoComTituloMuitoLongoLancaIae() {
        String longo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Lembrete(USER_ID, longo, null, DATA, Prioridade.BAIXA))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorCriacaoComDescricaoMuitoLongaLancaIae() {
        String longa = "a".repeat(501);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Lembrete(USER_ID, "T", longa, DATA, Prioridade.BAIXA))
                .withMessageContaining("descricao");
    }

    @Test
    void construtorCriacaoComDataNulaLancaNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(USER_ID, "T", null, null, Prioridade.BAIXA))
                .withMessageContaining("dataLembrete");
    }

    @Test
    void construtorCriacaoComPrioridadeNulaLancaNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Lembrete(USER_ID, "T", null, DATA, null))
                .withMessageContaining("prioridade");
    }

    @Test
    void atualizarMudaCamposEAtualizaTimestamp() throws InterruptedException {
        Lembrete l = new Lembrete(USER_ID, "Antigo", null, DATA, Prioridade.BAIXA);
        Instant antes = l.getAtualizadoEm();
        Thread.sleep(5);

        LocalDate nova = LocalDate.of(2026, 7, 1);
        l.atualizar("Novo", "Desc nova", nova, Prioridade.ALTA, true);

        assertThat(l.getTitulo()).isEqualTo("Novo");
        assertThat(l.getDescricao()).isEqualTo("Desc nova");
        assertThat(l.getDataLembrete()).isEqualTo(nova);
        assertThat(l.getPrioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(l.isConcluido()).isTrue();
        assertThat(l.getAtualizadoEm()).isAfter(antes);
    }

    @Test
    void atualizarComTituloInvalidoLancaIae() {
        Lembrete l = new Lembrete(USER_ID, "T", null, DATA, Prioridade.BAIXA);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> l.atualizar("   ", null, DATA, Prioridade.BAIXA, false));
    }

    @Test
    void equalsBaseadoNoId() {
        Lembrete a = new Lembrete(USER_ID, "T", null, DATA, Prioridade.BAIXA);
        Lembrete b = new Lembrete(a.getId(), a.getUserId(), "Outro", null, DATA,
                Prioridade.ALTA, true, a.getCriadoEm(), a.getAtualizadoEm());
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
