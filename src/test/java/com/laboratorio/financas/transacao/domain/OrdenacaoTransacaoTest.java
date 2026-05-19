package com.laboratorio.financas.transacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrdenacaoTransacaoTest {

    @Test
    void fromStringResolveNomeLogicoEmLowercase() {
        assertThat(OrdenacaoTransacao.fromString("data")).isEqualTo(OrdenacaoTransacao.DATA);
        assertThat(OrdenacaoTransacao.fromString("valor")).isEqualTo(OrdenacaoTransacao.VALOR);
        assertThat(OrdenacaoTransacao.fromString("descricao")).isEqualTo(OrdenacaoTransacao.DESCRICAO);
        assertThat(OrdenacaoTransacao.fromString("tipo")).isEqualTo(OrdenacaoTransacao.TIPO);
        assertThat(OrdenacaoTransacao.fromString("status")).isEqualTo(OrdenacaoTransacao.STATUS);
    }

    @Test
    void fromStringEhCaseInsensitive() {
        assertThat(OrdenacaoTransacao.fromString("VALOR")).isEqualTo(OrdenacaoTransacao.VALOR);
        assertThat(OrdenacaoTransacao.fromString("Data")).isEqualTo(OrdenacaoTransacao.DATA);
    }

    @Test
    void fromStringIgnoraEspacosAoRedor() {
        assertThat(OrdenacaoTransacao.fromString("  valor  ")).isEqualTo(OrdenacaoTransacao.VALOR);
    }

    @Test
    void fromStringLancaExcecaoParaCampoForaDoEnum() {
        assertThatThrownBy(() -> OrdenacaoTransacao.fromString("contaId"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contaId");
    }

    @Test
    void fromStringLancaExcecaoParaCampoNulo() {
        assertThatThrownBy(() -> OrdenacaoTransacao.fromString(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromStringLancaExcecaoParaCampoVazio() {
        assertThatThrownBy(() -> OrdenacaoTransacao.fromString("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nomeLogicoNaoCarregaPathDePersistencia() {
        // O enum de dominio nunca expoe o path JPA do @Embedded (valor.valor).
        for (OrdenacaoTransacao ordenacao : OrdenacaoTransacao.values()) {
            assertThat(ordenacao.nomeLogico()).doesNotContain(".");
        }
    }

    @Test
    void padraoEhData() {
        assertThat(OrdenacaoTransacao.PADRAO).isEqualTo(OrdenacaoTransacao.DATA);
    }
}
