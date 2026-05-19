package com.laboratorio.financas.transacao.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.transacao.domain.OrdenacaoTransacao;
import org.junit.jupiter.api.Test;

class OrdenacaoTransacaoJpaPathTest {

    @Test
    void resolverValorRetornaPathDoEmbeddable() {
        // valor e um @Embedded MoneyEmbeddable: a infra conhece o path interno.
        assertThat(OrdenacaoTransacaoJpaPath.resolver(OrdenacaoTransacao.VALOR))
                .isEqualTo("valor.valor");
    }

    @Test
    void resolverCamposSimplesRetornaNomeDireto() {
        assertThat(OrdenacaoTransacaoJpaPath.resolver(OrdenacaoTransacao.DATA)).isEqualTo("data");
        assertThat(OrdenacaoTransacaoJpaPath.resolver(OrdenacaoTransacao.DESCRICAO))
                .isEqualTo("descricao");
        assertThat(OrdenacaoTransacaoJpaPath.resolver(OrdenacaoTransacao.TIPO)).isEqualTo("tipo");
        assertThat(OrdenacaoTransacaoJpaPath.resolver(OrdenacaoTransacao.STATUS)).isEqualTo("status");
    }

    @Test
    void resolverCobreTodosOsValoresDoEnum() {
        for (OrdenacaoTransacao ordenacao : OrdenacaoTransacao.values()) {
            assertThat(OrdenacaoTransacaoJpaPath.resolver(ordenacao)).isNotBlank();
        }
    }
}
