package com.laboratorio.financas.fatura.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FaturaTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTA_ID = UUID.randomUUID();
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 6, 10);
    private static final Money VALOR = new Money(new BigDecimal("1500.00"), Currency.getInstance("BRL"));

    // --- Construtor de criacao ---

    @Test
    void construtorNovoComArgumentosValidosCriaFaturaNaoPaga() {
        Instant antes = Instant.now();
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao Maio", VENCIMENTO, null, VALOR);
        Instant depois = Instant.now();

        assertThat(fatura.getId()).isNotNull();
        assertThat(fatura.getUserId()).isEqualTo(USER_ID);
        assertThat(fatura.getContaId()).isEqualTo(CONTA_ID);
        assertThat(fatura.getNome()).isEqualTo("Cartao Maio");
        assertThat(fatura.getDataVencimento()).isEqualTo(VENCIMENTO);
        assertThat(fatura.getDataFechamento()).isNull();
        assertThat(fatura.getValorTotal()).isEqualTo(VALOR);
        assertThat(fatura.isPaga()).isFalse();
        assertThat(fatura.getCriadoEm()).isBetween(antes, depois);
        assertThat(fatura.getAtualizadoEm()).isEqualTo(fatura.getCriadoEm());
    }

    @Test
    void construtorNovoComValorTotalNuloPreservaNulo() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        assertThat(fatura.getValorTotal()).isNull();
    }

    @Test
    void construtorNovoComDataFechamentoPreservaCampo() {
        LocalDate fechamento = LocalDate.of(2026, 6, 3);
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, fechamento, null);
        assertThat(fatura.getDataFechamento()).isEqualTo(fechamento);
    }

    @Test
    void construtorNovoTrimaNome() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "  Cartao  ", VENCIMENTO, null, null);
        assertThat(fatura.getNome()).isEqualTo("Cartao");
    }

    @Test
    void construtorNovoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fatura(null, CONTA_ID, "Cartao", VENCIMENTO, null, null))
                .withMessageContaining("userId");
    }

    @Test
    void construtorNovoComContaIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fatura(USER_ID, null, "Cartao", VENCIMENTO, null, null))
                .withMessageContaining("contaId");
    }

    @Test
    void construtorNovoComDataVencimentoNulaLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fatura(USER_ID, CONTA_ID, "Cartao", null, null, null))
                .withMessageContaining("dataVencimento");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fatura(USER_ID, CONTA_ID, null, VENCIMENTO, null, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Fatura(USER_ID, CONTA_ID, "   ", VENCIMENTO, null, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Fatura(USER_ID, CONTA_ID, nomeLongo, VENCIMENTO, null, null))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, nome100, VENCIMENTO, null, null);
        assertThat(fatura.getNome()).hasSize(100);
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoPreservaTodosCampos() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-01-02T12:00:00Z");
        LocalDate fechamento = LocalDate.of(2026, 1, 3);

        Fatura fatura = new Fatura(id, USER_ID, CONTA_ID, "Cartao", VENCIMENTO, fechamento,
                VALOR, true, criadoEm, atualizadoEm);

        assertThat(fatura.getId()).isEqualTo(id);
        assertThat(fatura.getUserId()).isEqualTo(USER_ID);
        assertThat(fatura.getContaId()).isEqualTo(CONTA_ID);
        assertThat(fatura.getNome()).isEqualTo("Cartao");
        assertThat(fatura.getDataVencimento()).isEqualTo(VENCIMENTO);
        assertThat(fatura.getDataFechamento()).isEqualTo(fechamento);
        assertThat(fatura.getValorTotal()).isEqualTo(VALOR);
        assertThat(fatura.isPaga()).isTrue();
        assertThat(fatura.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(fatura.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fatura(null, USER_ID, CONTA_ID, "Cartao", VENCIMENTO,
                        null, null, false, Instant.now(), null))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Fatura(UUID.randomUUID(), USER_ID, CONTA_ID, "Cartao",
                        VENCIMENTO, null, null, false, null, null))
                .withMessageContaining("criadoEm");
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmNuloDefaultaParaCriadoEm() {
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Fatura fatura = new Fatura(UUID.randomUUID(), USER_ID, CONTA_ID, "Cartao", VENCIMENTO,
                null, null, false, criadoEm, null);
        assertThat(fatura.getAtualizadoEm()).isEqualTo(criadoEm);
    }

    // --- pagar() ---

    @Test
    void pagarMarcaComoPagaEAtualizaTimestamp() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        Instant atualizadoAntes = fatura.getAtualizadoEm();

        fatura.pagar();

        assertThat(fatura.isPaga()).isTrue();
        assertThat(fatura.getAtualizadoEm()).isAfterOrEqualTo(atualizadoAntes);
    }

    // --- atualizar() ---

    @Test
    void atualizarAlteraCamposEAtualizaTimestamp() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        LocalDate novoVencimento = LocalDate.of(2026, 7, 10);
        LocalDate novoFechamento = LocalDate.of(2026, 7, 3);
        Money novoValor = new Money(new BigDecimal("2000.00"), Currency.getInstance("BRL"));

        fatura.atualizar("Cartao Julho", novoVencimento, novoFechamento, novoValor);

        assertThat(fatura.getNome()).isEqualTo("Cartao Julho");
        assertThat(fatura.getDataVencimento()).isEqualTo(novoVencimento);
        assertThat(fatura.getDataFechamento()).isEqualTo(novoFechamento);
        assertThat(fatura.getValorTotal()).isEqualTo(novoValor);
    }

    @Test
    void atualizarComNomeNuloLancaNullPointerException() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        assertThatNullPointerException()
                .isThrownBy(() -> fatura.atualizar(null, VENCIMENTO, null, null))
                .withMessageContaining("nome");
    }

    @Test
    void atualizarComDataVencimentoNulaLancaNullPointerException() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        assertThatNullPointerException()
                .isThrownBy(() -> fatura.atualizar("Cartao", null, null, null))
                .withMessageContaining("dataVencimento");
    }

    @Test
    void atualizarPermiteZerarValorTotal() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, VALOR);
        fatura.atualizar("Cartao", VENCIMENTO, null, null);
        assertThat(fatura.getValorTotal()).isNull();
    }

    // --- equals e hashCode ---

    @Test
    void duasFaturasComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Fatura f1 = new Fatura(id, USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null, false, t, t);
        Fatura f2 = new Fatura(id, UUID.randomUUID(), UUID.randomUUID(), "Outro", VENCIMENTO,
                null, null, true, t, t);
        assertThat(f1).isEqualTo(f2);
        assertThat(f1.hashCode()).isEqualTo(f2.hashCode());
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        assertThat(fatura.equals(null)).isFalse();
        assertThat(fatura.equals("string")).isFalse();
    }

    @Test
    void toStringContemIdNomeEPaga() {
        Fatura fatura = new Fatura(USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null);
        String resultado = fatura.toString();
        assertThat(resultado)
                .contains(fatura.getId().toString())
                .contains("Cartao")
                .contains("paga");
    }
}
