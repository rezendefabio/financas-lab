package com.laboratorio.financas.transacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransacaoTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money VALOR_100 = new Money(new BigDecimal("100.00"), BRL);
    private static final LocalDate HOJE = LocalDate.now();
    private static final UUID CONTA_A = UUID.randomUUID();
    private static final UUID CONTA_B = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();

    // --- Construtor "novo" ---

    @Test
    void construtorNovoReceita_comCategoriaId_construiCorretamente() {
        // When
        Transacao t = new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", CONTA_A, null, CATEGORIA);

        // Then
        assertThat(t.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(t.getValor()).isEqualTo(VALOR_100);
        assertThat(t.getData()).isEqualTo(HOJE);
        assertThat(t.getDescricao()).isEqualTo("Salario");
        assertThat(t.getContaId()).isEqualTo(CONTA_A);
        assertThat(t.getContaDestinoId()).isNull();
        assertThat(t.getCategoriaId()).isEqualTo(CATEGORIA);
    }

    @Test
    void construtorNovoReceita_semCategoriaId_construiCorretamente() {
        // When
        Transacao t = new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Bonus", CONTA_A, null, null);

        // Then
        assertThat(t.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(t.getCategoriaId()).isNull();
    }

    @Test
    void construtorNovoDespesa_comCategoriaId_construiCorretamente() {
        // When
        Transacao t = new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Aluguel", CONTA_A, null, CATEGORIA);

        // Then
        assertThat(t.getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(t.getContaDestinoId()).isNull();
        assertThat(t.getCategoriaId()).isEqualTo(CATEGORIA);
    }

    @Test
    void construtorNovoTransferencia_comContaDestino_construiCorretamente() {
        // When
        Transacao t = new Transacao(TipoTransacao.TRANSFERENCIA, VALOR_100, HOJE, "TED", CONTA_A, CONTA_B, null);

        // Then
        assertThat(t.getTipo()).isEqualTo(TipoTransacao.TRANSFERENCIA);
        assertThat(t.getContaDestinoId()).isEqualTo(CONTA_B);
        assertThat(t.getCategoriaId()).isNull();
    }

    @Test
    void construtorNovoGeraIdEDefineCriadoEmEAtualizadoEm() {
        // When
        Instant antes = Instant.now().minusMillis(1);
        Transacao t = new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", CONTA_A, null, null);
        Instant depois = Instant.now().plusMillis(1);

        // Then
        assertThat(t.getId()).isNotNull();
        assertThat(t.getCriadoEm()).isBetween(antes, depois);
        assertThat(t.getAtualizadoEm()).isCloseTo(t.getCriadoEm(), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoTipoNulo() {
        assertThatThrownBy(() ->
            new Transacao(null, VALOR_100, HOJE, "Desc", CONTA_A, null, null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoValorNulo() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, null, HOJE, "Desc", CONTA_A, null, null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoDataNula() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, VALOR_100, null, "Desc", CONTA_A, null, null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoDescricaoNula() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, null, CONTA_A, null, null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoDescricaoBlank() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "   ", CONTA_A, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoDescricaoMaior200Chars() {
        // Given
        String descricaoLonga = "A".repeat(201);

        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, descricaoLonga, CONTA_A, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoContaIdNulo() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", null, null, null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoValorZero() {
        // Given
        Money valorZero = new Money(BigDecimal.ZERO, BRL);

        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, valorZero, HOJE, "Desc", CONTA_A, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoValorNegativo() {
        // Given
        Money valorNegativo = new Money(new BigDecimal("-10.00"), BRL);

        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, valorNegativo, HOJE, "Desc", CONTA_A, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoAceitaDescricaoComExatamente200Chars() {
        // Given
        String descricao200 = "A".repeat(200);

        // When
        Transacao t = new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, descricao200, CONTA_A, null, null);

        // Then
        assertThat(t.getDescricao()).hasSize(200);
    }

    @Test
    void construtorNovoAceitaDescricaoComUmChar() {
        // When
        Transacao t = new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "X", CONTA_A, null, null);

        // Then
        assertThat(t.getDescricao()).isEqualTo("X");
    }

    // --- Validacoes cruzadas ---

    @Test
    void receitaComContaDestinoIdLancaIllegalArgumentException() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", CONTA_A, CONTA_B, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void despesaComContaDestinoIdLancaIllegalArgumentException() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Desc", CONTA_A, CONTA_B, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferenciaComContaDestinoIdNuloLancaIllegalArgumentException() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.TRANSFERENCIA, VALOR_100, HOJE, "TED", CONTA_A, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferenciaComContaDestinoIgualContaIdLancaIllegalArgumentException() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.TRANSFERENCIA, VALOR_100, HOJE, "TED", CONTA_A, CONTA_A, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferenciaComCategoriaIdLancaIllegalArgumentException() {
        assertThatThrownBy(() ->
            new Transacao(TipoTransacao.TRANSFERENCIA, VALOR_100, HOJE, "TED", CONTA_A, CONTA_B, CATEGORIA)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void receitaSemCategoriaIdAceita() {
        // When/Then
        Transacao t = new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Bonus", CONTA_A, null, null);
        assertThat(t.getCategoriaId()).isNull();
    }

    @Test
    void despesaSemCategoriaIdAceita() {
        // When/Then
        Transacao t = new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Mercado", CONTA_A, null, null);
        assertThat(t.getCategoriaId()).isNull();
    }

    // --- Construtor "reconstrucao" ---

    @Test
    void construtorReconstrucaoPreservaTodosOsCampos() {
        // Given
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.now().minusSeconds(60);
        Instant atualizadoEm = Instant.now();

        // When
        Transacao t = new Transacao(
                id, TipoTransacao.DESPESA, VALOR_100, HOJE, "Conta de luz",
                CONTA_A, null, CATEGORIA, criadoEm, atualizadoEm
        );

        // Then
        assertThat(t.getId()).isEqualTo(id);
        assertThat(t.getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(t.getValor()).isEqualTo(VALOR_100);
        assertThat(t.getData()).isEqualTo(HOJE);
        assertThat(t.getDescricao()).isEqualTo("Conta de luz");
        assertThat(t.getContaId()).isEqualTo(CONTA_A);
        assertThat(t.getContaDestinoId()).isNull();
        assertThat(t.getCategoriaId()).isEqualTo(CATEGORIA);
        assertThat(t.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(t.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoLancaNullPointerExceptionQuandoIdNulo() {
        assertThatThrownBy(() ->
            new Transacao(
                null, TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, null, Instant.now(), null
            )
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorReconstrucaoLancaNullPointerExceptionQuandoCriadoEmNulo() {
        assertThatThrownBy(() ->
            new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, null, null, null
            )
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorReconstrucaoAceitaAtualizadoEmNuloDefaultandoParaCriadoEm() {
        // Given
        Instant criadoEm = Instant.now();

        // When
        Transacao t = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, null, criadoEm, null
        );

        // Then
        assertThat(t.getAtualizadoEm()).isEqualTo(criadoEm);
    }

    // --- Igualdade e toString ---

    @Test
    void duasTransacoesComMesmoIdSaoIguais() {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Transacao t1 = new Transacao(
                id, TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", CONTA_A, null, null, now, now
        );
        Transacao t2 = new Transacao(
                id, TipoTransacao.DESPESA, VALOR_100, HOJE, "Outra", CONTA_B, null, null, now, now
        );

        // Then
        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    void transacoesComIdsDiferentesNaoSaoIguais() {
        // Given
        Instant now = Instant.now();
        Transacao t1 = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", CONTA_A, null, null, now, now
        );
        Transacao t2 = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", CONTA_A, null, null, now, now
        );

        // Then
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void toStringContemIdTipoValorEData() {
        // When
        Transacao t = new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", CONTA_A, null, null);
        String str = t.toString();

        // Then
        assertThat(str).contains(t.getId().toString());
        assertThat(str).contains("RECEITA");
        assertThat(str).contains(HOJE.toString());
    }
}
