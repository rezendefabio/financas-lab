package com.laboratorio.financas.transacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FiltroTransacaoCampoTest {

    @Test
    void fromNomeResolveCamposDaWhitelist() {
        assertThat(FiltroTransacaoCampo.fromNome("descricao")).isEqualTo(FiltroTransacaoCampo.DESCRICAO);
        assertThat(FiltroTransacaoCampo.fromNome("valor")).isEqualTo(FiltroTransacaoCampo.VALOR);
        assertThat(FiltroTransacaoCampo.fromNome("data")).isEqualTo(FiltroTransacaoCampo.DATA);
    }

    @Test
    void fromNomeRejeitaCampoForaDaWhitelist() {
        assertThatThrownBy(() -> FiltroTransacaoCampo.fromNome("contaId"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao permitido");
    }

    @Test
    void cadaCampoTemTipoCorreto() {
        assertThat(FiltroTransacaoCampo.DESCRICAO.tipo()).isEqualTo(FiltroTransacaoCampo.Tipo.STRING);
        assertThat(FiltroTransacaoCampo.VALOR.tipo()).isEqualTo(FiltroTransacaoCampo.Tipo.NUMBER);
        assertThat(FiltroTransacaoCampo.DATA.tipo()).isEqualTo(FiltroTransacaoCampo.Tipo.DATE);
    }

    @Test
    void operadoresValidosDeStringIncluemContains() {
        assertThat(FiltroTransacaoCampo.DESCRICAO.operadoresValidos())
                .containsExactlyInAnyOrder("contains", "not_contains", "eq", "neq");
    }

    @Test
    void operadoresValidosDeNumberEDateSaoComparacao() {
        assertThat(FiltroTransacaoCampo.VALOR.operadoresValidos())
                .containsExactlyInAnyOrder("eq", "neq", "gt", "gte", "lt", "lte");
        assertThat(FiltroTransacaoCampo.DATA.operadoresValidos())
                .containsExactlyInAnyOrder("eq", "neq", "gt", "gte", "lt", "lte");
    }

    @Test
    void validarAceitaFiltroStringContains() {
        assertThatCode(() ->
                FiltroTransacaoCampo.validar(new FiltroGenerico("descricao", "contains", "mercado")))
                .doesNotThrowAnyException();
    }

    @Test
    void validarAceitaFiltroNumeroGte() {
        assertThatCode(() ->
                FiltroTransacaoCampo.validar(new FiltroGenerico("valor", "gte", "100.50")))
                .doesNotThrowAnyException();
    }

    @Test
    void validarAceitaFiltroDataLte() {
        assertThatCode(() ->
                FiltroTransacaoCampo.validar(new FiltroGenerico("data", "lte", "2026-01-31")))
                .doesNotThrowAnyException();
    }

    @Test
    void validarRejeitaCampoForaDaWhitelist() {
        assertThatThrownBy(() ->
                FiltroTransacaoCampo.validar(new FiltroGenerico("contaId", "eq", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao permitido");
    }

    @Test
    void validarRejeitaOperadorIncompativelComTipo() {
        // contains nao e operador valido para campo numerico.
        assertThatThrownBy(() ->
                FiltroTransacaoCampo.validar(new FiltroGenerico("valor", "contains", "100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalido");
    }

    @Test
    void validarRejeitaValorNumericoNaoParseavel() {
        assertThatThrownBy(() ->
                FiltroTransacaoCampo.validar(new FiltroGenerico("valor", "gte", "abc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numerico invalido");
    }

    @Test
    void validarRejeitaValorDeDataNaoParseavel() {
        assertThatThrownBy(() ->
                FiltroTransacaoCampo.validar(new FiltroGenerico("data", "gte", "31-01-2026")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data invalido");
    }
}
