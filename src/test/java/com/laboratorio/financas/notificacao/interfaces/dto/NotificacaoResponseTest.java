package com.laboratorio.financas.notificacao.interfaces.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificacaoResponseTest {

    @Test
    void fromDomainMapeiaTodosOsCampos() {
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        Notificacao domain = new Notificacao(userId, TipoNotificacao.ORCAMENTO_EXCEDIDO, refId,
                "Orcamento excedido", "Alimentacao: 120% utilizado");

        NotificacaoResponse response = NotificacaoResponse.fromDomain(domain);

        assertThat(response.id()).isEqualTo(domain.getId());
        assertThat(response.tipo()).isEqualTo(TipoNotificacao.ORCAMENTO_EXCEDIDO);
        assertThat(response.referenciaId()).isEqualTo(refId);
        assertThat(response.titulo()).isEqualTo("Orcamento excedido");
        assertThat(response.descricao()).isEqualTo("Alimentacao: 120% utilizado");
        assertThat(response.criadoEm()).isEqualTo(domain.getCriadoEm().toString());
    }
}
