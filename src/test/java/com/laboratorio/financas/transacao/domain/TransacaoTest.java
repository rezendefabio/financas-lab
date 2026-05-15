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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransacaoTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money VALOR_100 = new Money(new BigDecimal("100.00"), BRL);
    private static final LocalDate HOJE = LocalDate.now();
    private static final UUID CONTA_A = UUID.randomUUID();
    private static final UUID CONTA_B = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Transacao receitaSimples() {
        return new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", CONTA_A,
                CATEGORIA, USER_ID, StatusTransacao.CLEARED, null, List.of()
        );
    }

    private Transacao reconstruir(UUID id) {
        return new Transacao(
                id, TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario",
                CONTA_A, CATEGORIA, Instant.now(), null,
                USER_ID, StatusTransacao.CLEARED, null, null, null, null, List.of()
        );
    }

    // --- Construtor "novo" ---

    @Test
    void construtorNovoReceitaComCategoriaIdConstruiCorretamente() {
        Transacao t = receitaSimples();

        assertThat(t.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(t.getValor()).isEqualTo(VALOR_100);
        assertThat(t.getData()).isEqualTo(HOJE);
        assertThat(t.getDescricao()).isEqualTo("Salario");
        assertThat(t.getContaId()).isEqualTo(CONTA_A);
        assertThat(t.getCategoriaId()).isEqualTo(CATEGORIA);
    }

    @Test
    void construtorNovoReceitaSemCategoriaIdConstruiCorretamente() {
        Transacao t = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Bonus", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        );

        assertThat(t.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(t.getCategoriaId()).isNull();
    }

    @Test
    void construtorNovoDespesaComCategoriaIdConstruiCorretamente() {
        Transacao t = new Transacao(
                TipoTransacao.DESPESA, VALOR_100, HOJE, "Aluguel", CONTA_A,
                CATEGORIA, USER_ID, StatusTransacao.CLEARED, null, List.of()
        );

        assertThat(t.getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(t.getCategoriaId()).isEqualTo(CATEGORIA);
    }

    @Test
    void construtorNovoGeraIdEDefineCriadoEmEAtualizadoEm() {
        Instant antes = Instant.now().minusMillis(1);
        Transacao t = receitaSimples();
        Instant depois = Instant.now().plusMillis(1);

        assertThat(t.getId()).isNotNull();
        assertThat(t.getCriadoEm()).isBetween(antes, depois);
        assertThat(t.getAtualizadoEm()).isCloseTo(t.getCriadoEm(), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoTipoNulo() {
        assertThatThrownBy(() -> new Transacao(
                null, VALOR_100, HOJE, "Desc", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoValorNulo() {
        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, null, HOJE, "Desc", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoDataNula() {
        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, VALOR_100, null, "Desc", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoDescricaoNula() {
        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, null, CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoDescricaoBlank() {
        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "   ", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoDescricaoMaior200Chars() {
        String descricaoLonga = "A".repeat(201);

        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, descricaoLonga, CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoLancaNullPointerExceptionQuandoContaIdNulo() {
        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", null,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoValorZero() {
        Money valorZero = new Money(BigDecimal.ZERO, BRL);

        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, valorZero, HOJE, "Desc", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoLancaIllegalArgumentExceptionQuandoValorNegativo() {
        Money valorNegativo = new Money(new BigDecimal("-10.00"), BRL);

        assertThatThrownBy(() -> new Transacao(
                TipoTransacao.RECEITA, valorNegativo, HOJE, "Desc", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construtorNovoAceitaDescricaoComExatamente200Chars() {
        String descricao200 = "A".repeat(200);

        Transacao t = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, descricao200, CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        );

        assertThat(t.getDescricao()).hasSize(200);
    }

    @Test
    void construtorNovoAceitaDescricaoComUmChar() {
        Transacao t = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "X", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, List.of()
        );

        assertThat(t.getDescricao()).isEqualTo("X");
    }

    // --- Status default ---

    @Test
    void statusDefaultEClearedQuandoNaoInformado() {
        Transacao t = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, Instant.now(), null,
                USER_ID, null, null, null, null, null, List.of()
        );

        assertThat(t.getStatus()).isEqualTo(StatusTransacao.CLEARED);
    }

    @Test
    void statusPendingDefinidoCorretamente() {
        Transacao t = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", CONTA_A,
                null, USER_ID, StatusTransacao.PENDING, null, List.of()
        );

        assertThat(t.getStatus()).isEqualTo(StatusTransacao.PENDING);
    }

    // --- isDeleted ---

    @Test
    void isDeletedRetornaFalseQuandoDeletedAtNulo() {
        Transacao t = receitaSimples();

        assertThat(t.isDeleted()).isFalse();
    }

    @Test
    void isDeletedRetornaTrueQuandoDeletedAtDefinido() {
        Transacao t = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, Instant.now(), null,
                USER_ID, StatusTransacao.CLEARED, Instant.now(), null, null, null, List.of()
        );

        assertThat(t.isDeleted()).isTrue();
    }

    // --- criarParTransferencia ---

    @Test
    void criarParTransferenciaRetornaDespesaEReceita() {
        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                USER_ID, VALOR_100, CONTA_A, CONTA_B, HOJE, "TED", null
        );

        assertThat(par).isNotNull();
        assertThat(par.despesa()).isNotNull();
        assertThat(par.receita()).isNotNull();
    }

    @Test
    void criarParTransferenciaDespesaNaContaOrigem() {
        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                USER_ID, VALOR_100, CONTA_A, CONTA_B, HOJE, "TED", null
        );

        assertThat(par.despesa().getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(par.despesa().getContaId()).isEqualTo(CONTA_A);
    }

    @Test
    void criarParTransferenciaReceitaNaContaDestino() {
        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                USER_ID, VALOR_100, CONTA_A, CONTA_B, HOJE, "TED", null
        );

        assertThat(par.receita().getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(par.receita().getContaId()).isEqualTo(CONTA_B);
    }

    @Test
    void criarParTransferenciaCompartilhamTransferGroupId() {
        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                USER_ID, VALOR_100, CONTA_A, CONTA_B, HOJE, "TED", null
        );

        assertThat(par.despesa().getTransferGroupId()).isNotNull();
        assertThat(par.despesa().getTransferGroupId())
                .isEqualTo(par.receita().getTransferGroupId());
    }

    @Test
    void criarParTransferenciaTransferPairIdCruzado() {
        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                USER_ID, VALOR_100, CONTA_A, CONTA_B, HOJE, "TED", null
        );

        assertThat(par.despesa().getTransferPairId()).isEqualTo(par.receita().getId());
        assertThat(par.receita().getTransferPairId()).isEqualTo(par.despesa().getId());
    }

    @Test
    void criarParTransferenciaValoresIguais() {
        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                USER_ID, VALOR_100, CONTA_A, CONTA_B, HOJE, "TED", null
        );

        assertThat(par.despesa().getValor()).isEqualTo(VALOR_100);
        assertThat(par.receita().getValor()).isEqualTo(VALOR_100);
    }

    @Test
    void criarParTransferenciaLancaExcecaoQuandoContaOrigemIgualDestino() {
        assertThatThrownBy(() -> Transacao.criarParTransferencia(
                USER_ID, VALOR_100, CONTA_A, CONTA_A, HOJE, "TED", null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criarParTransferenciaLancaNPEQuandoContaOrigemNula() {
        assertThatThrownBy(() -> Transacao.criarParTransferencia(
                USER_ID, VALOR_100, null, CONTA_B, HOJE, "TED", null
        )).isInstanceOf(NullPointerException.class);
    }

    // --- Tags ---

    @Test
    void tagIdsDefaultVazioQuandoNaoInformado() {
        Transacao t = receitaSimples();

        assertThat(t.getTagIds()).isEmpty();
    }

    @Test
    void tagIdsImutaveis() {
        List<UUID> tags = new java.util.ArrayList<>();
        tags.add(UUID.randomUUID());
        Transacao t = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", CONTA_A,
                null, USER_ID, StatusTransacao.CLEARED, null, tags
        );

        assertThat(t.getTagIds()).hasSize(1);
    }

    // --- Construtor "reconstrucao" ---

    @Test
    void construtorReconstrucaoPreservaTodosOsCampos() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.now().minusSeconds(60);
        Instant atualizadoEm = Instant.now();
        UUID payeeId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID pairId = UUID.randomUUID();

        Transacao t = new Transacao(
                id, TipoTransacao.DESPESA, VALOR_100, HOJE, "Conta de luz",
                CONTA_A, CATEGORIA, criadoEm, atualizadoEm,
                USER_ID, StatusTransacao.PENDING, null, payeeId, groupId, pairId, List.of()
        );

        assertThat(t.getId()).isEqualTo(id);
        assertThat(t.getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(t.getValor()).isEqualTo(VALOR_100);
        assertThat(t.getData()).isEqualTo(HOJE);
        assertThat(t.getDescricao()).isEqualTo("Conta de luz");
        assertThat(t.getContaId()).isEqualTo(CONTA_A);
        assertThat(t.getCategoriaId()).isEqualTo(CATEGORIA);
        assertThat(t.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(t.getAtualizadoEm()).isEqualTo(atualizadoEm);
        assertThat(t.getUserId()).isEqualTo(USER_ID);
        assertThat(t.getStatus()).isEqualTo(StatusTransacao.PENDING);
        assertThat(t.getPayeeId()).isEqualTo(payeeId);
        assertThat(t.getTransferGroupId()).isEqualTo(groupId);
        assertThat(t.getTransferPairId()).isEqualTo(pairId);
    }

    @Test
    void construtorReconstrucaoLancaNullPointerExceptionQuandoIdNulo() {
        assertThatThrownBy(() ->
                new Transacao(
                        null, TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                        CONTA_A, null, Instant.now(), null,
                        USER_ID, StatusTransacao.CLEARED, null, null, null, null, List.of()
                )
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorReconstrucaoLancaNullPointerExceptionQuandoCriadoEmNulo() {
        assertThatThrownBy(() ->
                new Transacao(
                        UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                        CONTA_A, null, null, null,
                        USER_ID, StatusTransacao.CLEARED, null, null, null, null, List.of()
                )
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void construtorReconstrucaoAceitaAtualizadoEmNuloDefaultandoParaCriadoEm() {
        Instant criadoEm = Instant.now();

        Transacao t = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, criadoEm, null,
                USER_ID, StatusTransacao.CLEARED, null, null, null, null, List.of()
        );

        assertThat(t.getAtualizadoEm()).isEqualTo(criadoEm);
    }

    // --- Igualdade e toString ---

    @Test
    void duasTransacoesComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Transacao t1 = new Transacao(
                id, TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc", CONTA_A, null, now, now,
                USER_ID, StatusTransacao.CLEARED, null, null, null, null, List.of()
        );
        Transacao t2 = new Transacao(
                id, TipoTransacao.DESPESA, VALOR_100, HOJE, "Outra", CONTA_B, null, now, now,
                null, StatusTransacao.PENDING, null, null, null, null, List.of()
        );

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    void transacoesComIdsDiferentesNaoSaoIguais() {
        Instant now = Instant.now();
        Transacao t1 = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, now, now,
                USER_ID, StatusTransacao.CLEARED, null, null, null, null, List.of()
        );
        Transacao t2 = new Transacao(
                UUID.randomUUID(), TipoTransacao.RECEITA, VALOR_100, HOJE, "Desc",
                CONTA_A, null, now, now,
                USER_ID, StatusTransacao.CLEARED, null, null, null, null, List.of()
        );

        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void toStringContemIdTipoValorEData() {
        Transacao t = receitaSimples();
        String str = t.toString();

        assertThat(str).contains(t.getId().toString());
        assertThat(str).contains("RECEITA");
        assertThat(str).contains(HOJE.toString());
    }
}
