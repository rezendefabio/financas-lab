package com.laboratorio.financas.lancamentorecorrente.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LancamentoRecorrenteTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money VALOR_100 = new Money(new BigDecimal("100.00"), BRL);
    private static final LocalDate PROXIMA = LocalDate.of(2026, 6, 1);

    private LancamentoRecorrente lancamentoValido() {
        return new LancamentoRecorrente(
                "Assinatura mensal",
                TipoTransacao.DESPESA,
                VALOR_100,
                UUID.randomUUID(),
                null,
                Periodicidade.MENSAL,
                PROXIMA
        );
    }

    @Nested
    class ConstrutorNovo {

        @Test
        void comArgumentosValidosCriaLancamentoAtivo() {
            Instant antes = Instant.now();
            LancamentoRecorrente l = lancamentoValido();
            Instant depois = Instant.now();

            assertThat(l.getId()).isNotNull();
            assertThat(l.getDescricao()).isEqualTo("Assinatura mensal");
            assertThat(l.getTipo()).isEqualTo(TipoTransacao.DESPESA);
            assertThat(l.getValor()).isEqualTo(VALOR_100);
            assertThat(l.getPeriodicidade()).isEqualTo(Periodicidade.MENSAL);
            assertThat(l.getProximaOcorrencia()).isEqualTo(PROXIMA);
            assertThat(l.isAtivo()).isTrue();
            assertThat(l.getCriadoEm()).isBetween(antes, depois);
            assertThat(l.getAtualizadoEm()).isBetween(antes, depois);
        }

        @Test
        void duosLancamentosTenIdsDiferentes() {
            LancamentoRecorrente l1 = lancamentoValido();
            LancamentoRecorrente l2 = lancamentoValido();

            assertThat(l1.getId()).isNotEqualTo(l2.getId());
        }

        @Test
        void comDescricaoNulaLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            null, TipoTransacao.DESPESA, VALOR_100,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA));
        }

        @Test
        void comDescricaoVaziaLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "   ", TipoTransacao.DESPESA, VALOR_100,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA));
        }

        @Test
        void comDescricaoAcima200CaracteresLancaException() {
            String longa = "x".repeat(201);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            longa, TipoTransacao.DESPESA, VALOR_100,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA));
        }

        @Test
        void comTipoTransferenciaLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "Transferencia", TipoTransacao.TRANSFERENCIA, VALOR_100,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA))
                    .withMessageContaining("TRANSFERENCIA");
        }

        @Test
        void comTipoNuloLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "Teste", null, VALOR_100,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA));
        }

        @Test
        void comValorNuloLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "Teste", TipoTransacao.DESPESA, null,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA));
        }

        @Test
        void comValorZeroLancaException() {
            Money zero = new Money(BigDecimal.ZERO, BRL);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "Teste", TipoTransacao.DESPESA, zero,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA));
        }

        @Test
        void comContaIdNuloLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "Teste", TipoTransacao.DESPESA, VALOR_100,
                            null, null, Periodicidade.MENSAL, PROXIMA));
        }

        @Test
        void comPeriodicidadeNulaLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "Teste", TipoTransacao.DESPESA, VALOR_100,
                            UUID.randomUUID(), null, null, PROXIMA));
        }

        @Test
        void comProximaOcorrenciaNulaLancaException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new LancamentoRecorrente(
                            "Teste", TipoTransacao.DESPESA, VALOR_100,
                            UUID.randomUUID(), null, Periodicidade.MENSAL, null));
        }

        @Test
        void tipoReceitaEhAceito() {
            LancamentoRecorrente l = new LancamentoRecorrente(
                    "Salario", TipoTransacao.RECEITA, VALOR_100,
                    UUID.randomUUID(), null, Periodicidade.MENSAL, PROXIMA);

            assertThat(l.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        }

        @Test
        void categoriaIdNullEhAceito() {
            LancamentoRecorrente l = lancamentoValido();

            assertThat(l.getCategoriaId()).isNull();
        }
    }

    @Nested
    class AvancarProximaOcorrencia {

        @Test
        void mensalAvancaUmMes() {
            LancamentoRecorrente l = new LancamentoRecorrente(
                    "Mensal", TipoTransacao.DESPESA, VALOR_100,
                    UUID.randomUUID(), null, Periodicidade.MENSAL, LocalDate.of(2026, 1, 1));

            l.avancarProximaOcorrencia();

            assertThat(l.getProximaOcorrencia()).isEqualTo(LocalDate.of(2026, 2, 1));
        }

        @Test
        void semanalAvancaUmaSemana() {
            LancamentoRecorrente l = new LancamentoRecorrente(
                    "Semanal", TipoTransacao.DESPESA, VALOR_100,
                    UUID.randomUUID(), null, Periodicidade.SEMANAL, LocalDate.of(2026, 1, 1));

            l.avancarProximaOcorrencia();

            assertThat(l.getProximaOcorrencia()).isEqualTo(LocalDate.of(2026, 1, 8));
        }

        @Test
        void anualAvancaUmAno() {
            LancamentoRecorrente l = new LancamentoRecorrente(
                    "Anual", TipoTransacao.DESPESA, VALOR_100,
                    UUID.randomUUID(), null, Periodicidade.ANUAL, LocalDate.of(2026, 1, 1));

            l.avancarProximaOcorrencia();

            assertThat(l.getProximaOcorrencia()).isEqualTo(LocalDate.of(2027, 1, 1));
        }

        @Test
        void avancarAtualizaAtualizadoEm() {
            LancamentoRecorrente l = lancamentoValido();
            Instant antes = l.getAtualizadoEm();

            l.avancarProximaOcorrencia();

            assertThat(l.getAtualizadoEm()).isAfterOrEqualTo(antes);
        }
    }

    @Nested
    class Desativar {

        @Test
        void desativarMudaAtivoParaFalse() {
            LancamentoRecorrente l = lancamentoValido();

            l.desativar();

            assertThat(l.isAtivo()).isFalse();
        }

        @Test
        void desativarAtualizaAtualizadoEm() {
            LancamentoRecorrente l = lancamentoValido();
            Instant antes = l.getAtualizadoEm();

            l.desativar();

            assertThat(l.getAtualizadoEm()).isAfterOrEqualTo(antes);
        }
    }

    @Nested
    class ConstrutorReconstrucao {

        @Test
        void comTodosOsCamposPreservaValores() {
            UUID id = UUID.randomUUID();
            UUID contaId = UUID.randomUUID();
            UUID categoriaId = UUID.randomUUID();
            Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
            Instant atualizadoEm = Instant.parse("2026-06-01T10:00:00Z");

            LancamentoRecorrente l = new LancamentoRecorrente(
                    id, "Teste", TipoTransacao.RECEITA, VALOR_100,
                    contaId, categoriaId, Periodicidade.TRIMESTRAL, PROXIMA,
                    false, criadoEm, atualizadoEm);

            assertThat(l.getId()).isEqualTo(id);
            assertThat(l.getDescricao()).isEqualTo("Teste");
            assertThat(l.getTipo()).isEqualTo(TipoTransacao.RECEITA);
            assertThat(l.getContaId()).isEqualTo(contaId);
            assertThat(l.getCategoriaId()).isEqualTo(categoriaId);
            assertThat(l.getPeriodicidade()).isEqualTo(Periodicidade.TRIMESTRAL);
            assertThat(l.isAtivo()).isFalse();
            assertThat(l.getCriadoEm()).isEqualTo(criadoEm);
            assertThat(l.getAtualizadoEm()).isEqualTo(atualizadoEm);
        }
    }
}
