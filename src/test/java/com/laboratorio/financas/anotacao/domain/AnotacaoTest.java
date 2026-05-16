package com.laboratorio.financas.anotacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnotacaoTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    void construtorCriacaoAtribuiCamposObrigatorios() {
        Anotacao anotacao = new Anotacao(
                USUARIO_ID,
                "Lembrar de pagar fatura",
                null,
                TipoAnotacao.LEMBRETE,
                PrioridadeAnotacao.ALTA,
                null,
                null
        );

        assertThat(anotacao.getId()).isNotNull();
        assertThat(anotacao.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(anotacao.getTitulo()).isEqualTo("Lembrar de pagar fatura");
        assertThat(anotacao.getConteudo()).isNull();
        assertThat(anotacao.getTipo()).isEqualTo(TipoAnotacao.LEMBRETE);
        assertThat(anotacao.getPrioridade()).isEqualTo(PrioridadeAnotacao.ALTA);
        assertThat(anotacao.getValor()).isNull();
        assertThat(anotacao.getDataReferencia()).isNull();
        assertThat(anotacao.getCriadoEm()).isNotNull();
        assertThat(anotacao.getAtualizadoEm()).isNotNull();
    }

    @Test
    void construtorCriacaoComTodosOsCampos() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        LocalDate dataReferencia = LocalDate.of(2026, 6, 1);

        Anotacao anotacao = new Anotacao(
                USUARIO_ID,
                "Planejamento mensal",
                "Detalhes do planejamento",
                TipoAnotacao.PLANEJAMENTO,
                PrioridadeAnotacao.MEDIA,
                valor,
                dataReferencia
        );

        assertThat(anotacao.getTitulo()).isEqualTo("Planejamento mensal");
        assertThat(anotacao.getConteudo()).isEqualTo("Detalhes do planejamento");
        assertThat(anotacao.getValor()).isEqualTo(valor);
        assertThat(anotacao.getDataReferencia()).isEqualTo(dataReferencia);
    }

    @Test
    void construtorCriacaoLancaExcecaoQuandoUsuarioIdNulo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anotacao(
                        null,
                        "Titulo",
                        null,
                        TipoAnotacao.LEMBRETE,
                        PrioridadeAnotacao.MEDIA,
                        null,
                        null
                ))
                .withMessageContaining("usuarioId");
    }

    @Test
    void construtorCriacaoLancaExcecaoQuandoTituloNulo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anotacao(
                        USUARIO_ID,
                        null,
                        null,
                        TipoAnotacao.LEMBRETE,
                        PrioridadeAnotacao.MEDIA,
                        null,
                        null
                ))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorCriacaoLancaExcecaoQuandoTituloVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anotacao(
                        USUARIO_ID,
                        "   ",
                        null,
                        TipoAnotacao.LEMBRETE,
                        PrioridadeAnotacao.MEDIA,
                        null,
                        null
                ))
                .withMessageContaining("titulo");
    }

    @Test
    void construtorCriacaoLancaExcecaoQuandoTipoNulo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anotacao(
                        USUARIO_ID,
                        "Titulo",
                        null,
                        null,
                        PrioridadeAnotacao.MEDIA,
                        null,
                        null
                ))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorCriacaoLancaExcecaoQuandoPrioridadeNula() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anotacao(
                        USUARIO_ID,
                        "Titulo",
                        null,
                        TipoAnotacao.LEMBRETE,
                        null,
                        null,
                        null
                ))
                .withMessageContaining("prioridade");
    }

    @Test
    void construtorReconstituicaoAceitaTodosOsCampos() {
        UUID id = UUID.randomUUID();
        java.time.Instant criadoEm = java.time.Instant.now();
        java.time.Instant atualizadoEm = java.time.Instant.now();

        Anotacao anotacao = new Anotacao(
                id,
                USUARIO_ID,
                "Titulo",
                "Conteudo",
                TipoAnotacao.OBSERVACAO,
                PrioridadeAnotacao.BAIXA,
                null,
                null,
                criadoEm,
                atualizadoEm
        );

        assertThat(anotacao.getId()).isEqualTo(id);
        assertThat(anotacao.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(anotacao.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void atualizarMudaCamposEAtualizaTimestamp() throws InterruptedException {
        Anotacao anotacao = new Anotacao(
                USUARIO_ID,
                "Titulo original",
                null,
                TipoAnotacao.LEMBRETE,
                PrioridadeAnotacao.BAIXA,
                null,
                null
        );
        java.time.Instant antes = anotacao.getAtualizadoEm();
        Thread.sleep(2);

        Money novoValor = new Money(new BigDecimal("50.00"), BRL);
        anotacao.atualizar(
                "Novo titulo",
                "Novo conteudo",
                TipoAnotacao.ALERTA,
                PrioridadeAnotacao.URGENTE,
                novoValor,
                LocalDate.of(2026, 7, 1)
        );

        assertThat(anotacao.getTitulo()).isEqualTo("Novo titulo");
        assertThat(anotacao.getConteudo()).isEqualTo("Novo conteudo");
        assertThat(anotacao.getTipo()).isEqualTo(TipoAnotacao.ALERTA);
        assertThat(anotacao.getPrioridade()).isEqualTo(PrioridadeAnotacao.URGENTE);
        assertThat(anotacao.getValor()).isEqualTo(novoValor);
        assertThat(anotacao.getDataReferencia()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(anotacao.getAtualizadoEm()).isAfter(antes);
    }

    @Test
    void atualizarLancaExcecaoQuandoTituloVazio() {
        Anotacao anotacao = new Anotacao(
                USUARIO_ID,
                "Titulo",
                null,
                TipoAnotacao.LEMBRETE,
                PrioridadeAnotacao.MEDIA,
                null,
                null
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> anotacao.atualizar(
                        "  ",
                        null,
                        TipoAnotacao.LEMBRETE,
                        PrioridadeAnotacao.MEDIA,
                        null,
                        null
                ))
                .withMessageContaining("titulo");
    }

    @Test
    void atualizarPermiteValorNuloParaRemoverValorMonetario() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        Anotacao anotacao = new Anotacao(
                USUARIO_ID,
                "Titulo",
                null,
                TipoAnotacao.LEMBRETE,
                PrioridadeAnotacao.MEDIA,
                valor,
                null
        );

        anotacao.atualizar("Titulo", null, TipoAnotacao.LEMBRETE, PrioridadeAnotacao.MEDIA, null, null);

        assertThat(anotacao.getValor()).isNull();
    }
}
