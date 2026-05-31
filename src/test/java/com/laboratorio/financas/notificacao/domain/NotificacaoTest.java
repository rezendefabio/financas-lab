package com.laboratorio.financas.notificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificacaoTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID REF_ID = UUID.randomUUID();

    @Test
    void construtorCriacaoComArgumentosValidosCriaNaoDescartada() {
        Notificacao n = new Notificacao(USER_ID, TipoNotificacao.ORCAMENTO_EXCEDIDO, REF_ID,
                "Orcamento excedido", "Alimentacao: 120% utilizado");

        assertThat(n.getId()).isNotNull();
        assertThat(n.getUserId()).isEqualTo(USER_ID);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacao.ORCAMENTO_EXCEDIDO);
        assertThat(n.getReferenciaId()).isEqualTo(REF_ID);
        assertThat(n.isDescartada()).isFalse();
        assertThat(n.getCriadoEm()).isNotNull();
    }

    @Test
    void construtorComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Notificacao(null, TipoNotificacao.META_VENCIDA, REF_ID, "T", "D"))
                .withMessageContaining("userId");
    }

    @Test
    void construtorComTituloVazioLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Notificacao(USER_ID, TipoNotificacao.META_VENCIDA, REF_ID, "  ", "D"))
                .withMessageContaining("titulo");
    }

    @Test
    void descartarMudaFlagParaTrue() {
        Notificacao n = new Notificacao(USER_ID, TipoNotificacao.META_VENCENDO, REF_ID, "Meta", "vence em 3 dias");

        n.descartar();

        assertThat(n.isDescartada()).isTrue();
    }

    @Test
    void atualizarTextoMudaTituloEDescricao() {
        Notificacao n = new Notificacao(USER_ID, TipoNotificacao.ORCAMENTO_ATENCAO, REF_ID, "Antigo", "antiga");

        n.atualizarTexto("Novo", "nova");

        assertThat(n.getTitulo()).isEqualTo("Novo");
        assertThat(n.getDescricao()).isEqualTo("nova");
    }

    @Test
    void atualizarTextoPreservaFlagDescartada() {
        Notificacao n = new Notificacao(USER_ID, TipoNotificacao.ORCAMENTO_ATENCAO, REF_ID, "T", "D");
        n.descartar();

        n.atualizarTexto("Outro titulo", "outra descricao");

        assertThat(n.isDescartada()).isTrue();
    }
}
