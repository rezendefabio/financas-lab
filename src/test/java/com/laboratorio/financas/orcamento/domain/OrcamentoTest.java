package com.laboratorio.financas.orcamento.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrcamentoTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final UUID CATEGORIA_ID = UUID.randomUUID();
    private static final Money LIMITE_100 = new Money(new BigDecimal("100.00"), BRL);

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaOrcamentoAtivo() {
        // Given
        LocalDate mes = LocalDate.of(2026, 5, 1);
        Instant antes = Instant.now();

        // When
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, mes);

        // Then
        Instant depois = Instant.now();
        assertThat(orcamento.getId()).isNotNull();
        assertThat(orcamento.getCategoriaId()).isEqualTo(CATEGORIA_ID);
        assertThat(orcamento.getValorLimite()).isEqualTo(LIMITE_100);
        assertThat(orcamento.getMesAno()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(orcamento.isAtivo()).isTrue();
        assertThat(orcamento.getCriadoEm()).isBetween(antes, depois);
        assertThat(orcamento.getAtualizadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorNovoComDoisOrcamentosGeraIdsDiferentes() {
        Orcamento o1 = new Orcamento(CATEGORIA_ID, LIMITE_100, LocalDate.of(2026, 1, 1));
        Orcamento o2 = new Orcamento(CATEGORIA_ID, LIMITE_100, LocalDate.of(2026, 2, 1));
        assertThat(o1.getId()).isNotEqualTo(o2.getId());
    }

    @Test
    void construtorNovoComCategoriaIdNuloLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Orcamento(null, LIMITE_100, LocalDate.of(2026, 5, 1)))
                .withMessageContaining("categoriaId");
    }

    @Test
    void construtorNovoComValorLimiteNuloLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Orcamento(CATEGORIA_ID, null, LocalDate.of(2026, 5, 1)))
                .withMessageContaining("valorLimite");
    }

    @Test
    void construtorNovoComValorLimiteZeroLancaIllegalArgumentException() {
        Money limiteZero = new Money(BigDecimal.ZERO, BRL);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Orcamento(CATEGORIA_ID, limiteZero, LocalDate.of(2026, 5, 1)))
                .withMessageContaining("valorLimite");
    }

    @Test
    void construtorNovoComValorLimiteNegativoLancaIllegalArgumentException() {
        Money limiteNegativo = new Money(new BigDecimal("-50.00"), BRL);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Orcamento(CATEGORIA_ID, limiteNegativo, LocalDate.of(2026, 5, 1)))
                .withMessageContaining("valorLimite");
    }

    @Test
    void construtorNovoComMesAnoNuloLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Orcamento(CATEGORIA_ID, LIMITE_100, null))
                .withMessageContaining("mesAno");
    }

    @Test
    void construtorNovoNormalizaMesAnoParaPrimeiroDiaDoDia() {
        LocalDate mesComDiaMeio = LocalDate.of(2026, 5, 15);
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, mesComDiaMeio);
        assertThat(orcamento.getMesAno()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void construtorNovoComDia1NaoAlteraMesAno() {
        LocalDate mesDia1 = LocalDate.of(2026, 5, 1);
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, mesDia1);
        assertThat(orcamento.getMesAno()).isEqualTo(mesDia1);
    }

    @Test
    void construtorNovoComUltimoDiaDeMesNormalizaParaDia1() {
        LocalDate ultimoDia = LocalDate.of(2026, 5, 31);
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, ultimoDia);
        assertThat(orcamento.getMesAno()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoPreservaTodosOsCampos() {
        // Given
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        Money limite = new Money(new BigDecimal("500.00"), BRL);
        LocalDate mes = LocalDate.of(2025, 12, 1);
        Instant criadoEm = Instant.parse("2025-12-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2025-12-15T08:30:00Z");

        // When
        Orcamento orcamento = new Orcamento(id, categoriaId, limite, mes, false, criadoEm, atualizadoEm);

        // Then
        assertThat(orcamento.getId()).isEqualTo(id);
        assertThat(orcamento.getCategoriaId()).isEqualTo(categoriaId);
        assertThat(orcamento.getValorLimite()).isEqualTo(limite);
        assertThat(orcamento.getMesAno()).isEqualTo(mes);
        assertThat(orcamento.isAtivo()).isFalse();
        assertThat(orcamento.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(orcamento.getAtualizadoEm()).isEqualTo(atualizadoEm);
    }

    @Test
    void construtorReconstrucaoComAtivoTruePreservaEstado() {
        Orcamento orcamento = new Orcamento(
                UUID.randomUUID(), CATEGORIA_ID, LIMITE_100,
                LocalDate.of(2026, 1, 1), true, Instant.now(), Instant.now());
        assertThat(orcamento.isAtivo()).isTrue();
    }

    // --- desativar() ---

    @Test
    void desativarOrcamentoAtivoMarcaComoInativo() {
        // Given
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, LocalDate.of(2026, 5, 1));
        assertThat(orcamento.isAtivo()).isTrue();

        // When
        orcamento.desativar();

        // Then
        assertThat(orcamento.isAtivo()).isFalse();
    }

    @Test
    void desativarAtualizaAtualizadoEm() {
        // Given
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, LocalDate.of(2026, 5, 1));
        Instant antes = orcamento.getAtualizadoEm();

        // When
        orcamento.desativar();

        // Then
        assertThat(orcamento.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void desativarPreservaDemaisCampos() {
        // Given
        LocalDate mes = LocalDate.of(2026, 5, 1);
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, mes);
        UUID idOriginal = orcamento.getId();
        Instant criadoEmOriginal = orcamento.getCriadoEm();

        // When
        orcamento.desativar();

        // Then
        assertThat(orcamento.getId()).isEqualTo(idOriginal);
        assertThat(orcamento.getCategoriaId()).isEqualTo(CATEGORIA_ID);
        assertThat(orcamento.getValorLimite()).isEqualTo(LIMITE_100);
        assertThat(orcamento.getMesAno()).isEqualTo(mes);
        assertThat(orcamento.getCriadoEm()).isEqualTo(criadoEmOriginal);
    }

    @Test
    void desativarOrcamentoJaInativoMantemInativo() {
        // Given
        Orcamento orcamento = new Orcamento(CATEGORIA_ID, LIMITE_100, LocalDate.of(2026, 5, 1));
        orcamento.desativar();

        // When
        orcamento.desativar();

        // Then
        assertThat(orcamento.isAtivo()).isFalse();
    }
}
