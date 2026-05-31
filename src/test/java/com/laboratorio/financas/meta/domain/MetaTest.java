package com.laboratorio.financas.meta.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MetaTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Money VALOR_ALVO_1000 = new Money(new BigDecimal("1000.00"), BRL);
    private static final Money VALOR_ALVO_500 = new Money(new BigDecimal("500.00"), BRL);
    private static final LocalDate PRAZO = LocalDate.of(2027, 12, 31);

    // --- Construtor "novo" ---

    @Nested
    class ConstrutorNovo {

        @Test
        void comArgumentosValidosCriaMetaEmAndamento() {
            Instant antes = Instant.now();

            Meta meta = new Meta(USER_ID, "Viagem Europa", VALOR_ALVO_1000, PRAZO);

            Instant depois = Instant.now();
            assertThat(meta.getId()).isNotNull();
            assertThat(meta.getUserId()).isEqualTo(USER_ID);
            assertThat(meta.getNome()).isEqualTo("Viagem Europa");
            assertThat(meta.getValorAlvo()).isEqualTo(VALOR_ALVO_1000);
            assertThat(meta.getValorAtual()).isEqualTo(new Money(BigDecimal.ZERO, BRL));
            assertThat(meta.getPrazo()).isEqualTo(PRAZO);
            assertThat(meta.getStatus()).isEqualTo(StatusMeta.EM_ANDAMENTO);
            assertThat(meta.getCriadoEm()).isBetween(antes, depois);
            assertThat(meta.getAtualizadoEm()).isBetween(antes, depois);
        }

        @Test
        void valorAtualInicialEhZeroNaMoedaDoValorAlvo() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);

            assertThat(meta.getValorAtual().valor()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(meta.getValorAtual().moeda()).isEqualTo(BRL);
        }

        @Test
        void duasMetasCriadasEmSequenciaTenIdsDiferentes() {
            Meta meta1 = new Meta(USER_ID, "Meta 1", VALOR_ALVO_1000, PRAZO);
            Meta meta2 = new Meta(USER_ID, "Meta 2", VALOR_ALVO_500, PRAZO);

            assertThat(meta1.getId()).isNotEqualTo(meta2.getId());
        }

        @Test
        void comUserIdNuloLancaNullPointerException() {
            org.assertj.core.api.Assertions.assertThatNullPointerException()
                    .isThrownBy(() -> new Meta(null, "Reserva", VALOR_ALVO_1000, PRAZO))
                    .withMessageContaining("userId");
        }

        @Test
        void comNomeNuloLancaIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Meta(USER_ID, null, VALOR_ALVO_1000, PRAZO))
                    .withMessageContaining("nome");
        }

        @Test
        void comNomeVazioLancaIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Meta(USER_ID, "", VALOR_ALVO_1000, PRAZO))
                    .withMessageContaining("nome");
        }

        @Test
        void comNomeSomenteEspacosLancaIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Meta(USER_ID, "   ", VALOR_ALVO_1000, PRAZO))
                    .withMessageContaining("nome");
        }

        @Test
        void comValorAlvoNuloLancaIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Meta(USER_ID, "Reserva", null, PRAZO))
                    .withMessageContaining("valorAlvo");
        }

        @Test
        void comValorAlvoZeroLancaIllegalArgumentException() {
            Money valorZero = new Money(BigDecimal.ZERO, BRL);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Meta(USER_ID, "Reserva", valorZero, PRAZO))
                    .withMessageContaining("valorAlvo");
        }

        @Test
        void comValorAlvoNegativoLancaIllegalArgumentException() {
            Money valorNegativo = new Money(new BigDecimal("-100.00"), BRL);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Meta(USER_ID, "Reserva", valorNegativo, PRAZO))
                    .withMessageContaining("valorAlvo");
        }

        @Test
        void comPrazoNuloLancaIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, null))
                    .withMessageContaining("prazo");
        }
    }

    // --- Construtor de reconstrucao ---

    @Nested
    class ConstrutorReconstrucao {

        @Test
        void comTodosOsCamposValidosPreservaValores() {
            UUID id = UUID.randomUUID();
            Money valorAtual = new Money(new BigDecimal("300.00"), BRL);
            Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
            Instant atualizadoEm = Instant.parse("2026-06-01T10:00:00Z");

            Meta meta = new Meta(id, USER_ID, "Viagem", VALOR_ALVO_1000, valorAtual, PRAZO,
                    StatusMeta.EM_ANDAMENTO, criadoEm, atualizadoEm);

            assertThat(meta.getId()).isEqualTo(id);
            assertThat(meta.getNome()).isEqualTo("Viagem");
            assertThat(meta.getValorAlvo()).isEqualTo(VALOR_ALVO_1000);
            assertThat(meta.getValorAtual()).isEqualTo(valorAtual);
            assertThat(meta.getPrazo()).isEqualTo(PRAZO);
            assertThat(meta.getStatus()).isEqualTo(StatusMeta.EM_ANDAMENTO);
            assertThat(meta.getCriadoEm()).isEqualTo(criadoEm);
            assertThat(meta.getAtualizadoEm()).isEqualTo(atualizadoEm);
        }

        @Test
        void comStatusCanceladaPreservaEstado() {
            UUID id = UUID.randomUUID();
            Instant t = Instant.now();

            Meta meta = new Meta(id, USER_ID, "Cancelada", VALOR_ALVO_1000,
                    new Money(BigDecimal.ZERO, BRL), PRAZO, StatusMeta.CANCELADA, t, t);

            assertThat(meta.getStatus()).isEqualTo(StatusMeta.CANCELADA);
        }

        @Test
        void comStatusConcluidaPreservaEstado() {
            UUID id = UUID.randomUUID();
            Instant t = Instant.now();

            Meta meta = new Meta(id, USER_ID, "Concluida", VALOR_ALVO_1000,
                    VALOR_ALVO_1000, PRAZO, StatusMeta.CONCLUIDA, t, t);

            assertThat(meta.getStatus()).isEqualTo(StatusMeta.CONCLUIDA);
        }
    }

    // --- registrarDeposito() ---

    @Nested
    class RegistrarDeposito {

        @Test
        void depositoValidoIncrementaValorAtual() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money deposito = new Money(new BigDecimal("200.00"), BRL);

            meta.registrarDeposito(deposito);

            assertThat(meta.getValorAtual().valor())
                    .isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        void depositoValidoAtualizaAtualizadoEm() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Instant antes = meta.getAtualizadoEm();
            Money deposito = new Money(new BigDecimal("100.00"), BRL);

            meta.registrarDeposito(deposito);

            assertThat(meta.getAtualizadoEm()).isAfterOrEqualTo(antes);
        }

        @Test
        void multiplosDepositosAcumulam() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money deposito1 = new Money(new BigDecimal("300.00"), BRL);
            Money deposito2 = new Money(new BigDecimal("400.00"), BRL);

            meta.registrarDeposito(deposito1);
            meta.registrarDeposito(deposito2);

            assertThat(meta.getValorAtual().valor())
                    .isEqualByComparingTo(new BigDecimal("700.00"));
        }

        @Test
        void depositoQueAtingeValorAlvoConclui() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money deposito = new Money(new BigDecimal("1000.00"), BRL);

            meta.registrarDeposito(deposito);

            assertThat(meta.getStatus()).isEqualTo(StatusMeta.CONCLUIDA);
        }

        @Test
        void depositoQueUltrapassaValorAlvoConclui() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money deposito = new Money(new BigDecimal("1500.00"), BRL);

            meta.registrarDeposito(deposito);

            assertThat(meta.getStatus()).isEqualTo(StatusMeta.CONCLUIDA);
        }

        @Test
        void depositoAbaixoDoValorAlvoMantemEmAndamento() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money deposito = new Money(new BigDecimal("999.99"), BRL);

            meta.registrarDeposito(deposito);

            assertThat(meta.getStatus()).isEqualTo(StatusMeta.EM_ANDAMENTO);
        }

        @Test
        void depositoNuloLancaIllegalArgumentException() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> meta.registrarDeposito(null))
                    .withMessageContaining("deposito");
        }

        @Test
        void depositoZeroLancaIllegalArgumentException() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money depositoZero = new Money(BigDecimal.ZERO, BRL);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> meta.registrarDeposito(depositoZero))
                    .withMessageContaining("deposito");
        }

        @Test
        void depositoNegativoLancaIllegalArgumentException() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money depositoNegativo = new Money(new BigDecimal("-50.00"), BRL);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> meta.registrarDeposito(depositoNegativo))
                    .withMessageContaining("deposito");
        }

        @Test
        void depositoComMoedaDiferenteLancaIllegalArgumentException() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Money depositoUsd = new Money(new BigDecimal("100.00"), USD);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> meta.registrarDeposito(depositoUsd))
                    .withMessageContaining("Moeda");
        }

        @Test
        void depositoEmMetaCanceladaLancaIllegalStateException() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            meta.cancelar();
            Money deposito = new Money(new BigDecimal("100.00"), BRL);

            assertThatIllegalStateException()
                    .isThrownBy(() -> meta.registrarDeposito(deposito))
                    .withMessageContaining("andamento");
        }

        @Test
        void depositoEmMetaConcluidaLancaIllegalStateException() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            meta.registrarDeposito(new Money(new BigDecimal("1000.00"), BRL));
            Money deposito = new Money(new BigDecimal("100.00"), BRL);

            assertThatIllegalStateException()
                    .isThrownBy(() -> meta.registrarDeposito(deposito))
                    .withMessageContaining("andamento");
        }
    }

    // --- cancelar() ---

    @Nested
    class Cancelar {

        @Test
        void cancelarMetaEmAndamentoMudaStatusParaCancelada() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);

            meta.cancelar();

            assertThat(meta.getStatus()).isEqualTo(StatusMeta.CANCELADA);
        }

        @Test
        void cancelarAtualizaAtualizadoEm() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            Instant antes = meta.getAtualizadoEm();

            meta.cancelar();

            assertThat(meta.getAtualizadoEm()).isAfterOrEqualTo(antes);
        }

        @Test
        void cancelarMetaJaCanceladaAinda() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            meta.cancelar();

            // segundo cancelar nao deve lancar excecao (nao ha restricao para isso)
            meta.cancelar();

            assertThat(meta.getStatus()).isEqualTo(StatusMeta.CANCELADA);
        }

        @Test
        void cancelarMetaConcluidaLancaIllegalStateException() {
            Meta meta = new Meta(USER_ID, "Reserva", VALOR_ALVO_1000, PRAZO);
            meta.registrarDeposito(new Money(new BigDecimal("1000.00"), BRL));

            assertThatIllegalStateException()
                    .isThrownBy(meta::cancelar)
                    .withMessageContaining("concluida");
        }
    }
}
