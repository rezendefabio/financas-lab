package com.laboratorio.financas.transacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FiltrosTransacaoTest {

    @Test
    void construtorCompletoNormalizaFiltrosAdicionaisNuloParaListaVazia() {
        FiltrosTransacao filtros = new FiltrosTransacao(
                null, null, null, null, null, null, null, null);

        assertThat(filtros.filtrosAdicionais()).isEmpty();
    }

    @Test
    void construtorCompletoPreservaFiltrosAdicionaisInformados() {
        FiltroGenerico fg = new FiltroGenerico("descricao", "contains", "mercado");

        FiltrosTransacao filtros = new FiltrosTransacao(
                null, null, null, null, null, null, null, List.of(fg));

        assertThat(filtros.filtrosAdicionais()).containsExactly(fg);
    }

    @Test
    void filtrosAdicionaisRetornadaEImutavel() {
        FiltrosTransacao filtros = new FiltrosTransacao(
                null, null, null, null, null, null, null,
                List.of(new FiltroGenerico("valor", "gte", "100")));

        assertThatThrownBy(() ->
                filtros.filtrosAdicionais().add(new FiltroGenerico("data", "lte", "2026-01-01")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copiaDefensivaIsolaDaListaOriginal() {
        List<FiltroGenerico> origem = new ArrayList<>();
        origem.add(new FiltroGenerico("descricao", "eq", "salario"));

        FiltrosTransacao filtros = new FiltrosTransacao(
                null, null, null, null, null, null, null, origem);
        origem.clear();

        assertThat(filtros.filtrosAdicionais()).hasSize(1);
    }

    @Test
    void construtorRetroativoDeCincoArgumentosDeixaFiltrosAdicionaisVazios() {
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        assertThat(filtros.filtrosAdicionais()).isEmpty();
        assertThat(filtros.userId()).isNull();
        assertThat(filtros.status()).isNull();
    }

    @Test
    void construtorRetroativoDeSeteArgumentosDeixaFiltrosAdicionaisVazios() {
        FiltrosTransacao filtros = new FiltrosTransacao(
                null, null, null, TipoTransacao.RECEITA, null, null, StatusTransacao.CLEARED);

        assertThat(filtros.filtrosAdicionais()).isEmpty();
        assertThat(filtros.tipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(filtros.status()).isEqualTo(StatusTransacao.CLEARED);
    }
}
