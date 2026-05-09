package com.laboratorio.financas.conta.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContaRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private ContaRepositoryImpl repository;

    @Autowired
    private ContaJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void salvarPersisteContaERetornaInstanciaEquivalente() {
        // Given
        Conta nova = new Conta(
                "Conta Corrente",
                TipoConta.CORRENTE,
                new Money(new BigDecimal("100.00"), BRL)
        );

        // When
        Conta salva = repository.salvar(nova);

        // Then
        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getNome()).isEqualTo("Conta Corrente");
        assertThat(salva.getTipo()).isEqualTo(TipoConta.CORRENTE);
        assertThat(salva.getSaldoInicial()).isEqualTo(new Money(new BigDecimal("100.00"), BRL));
        assertThat(salva.isAtiva()).isTrue();
    }

    @Test
    void salvarPersisteCorretamenteSaldoComDuasCasasDecimais() {
        // Given
        Conta nova = new Conta(
                "Poupanca",
                TipoConta.POUPANCA,
                new Money(new BigDecimal("100.50"), BRL)
        );

        // When
        Conta salva = repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(salva.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getSaldoInicial())
                .isEqualTo(new Money(new BigDecimal("100.50"), BRL));
    }

    @Test
    void salvarPersisteCorretamenteSaldoZero() {
        // Given
        Conta nova = new Conta(
                "Dinheiro Vivo",
                TipoConta.DINHEIRO,
                new Money(new BigDecimal("0.00"), BRL)
        );

        // When
        Conta salva = repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(salva.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getSaldoInicial())
                .isEqualTo(new Money(new BigDecimal("0.00"), BRL));
    }

    @Test
    void salvarPersisteCorretamenteSaldoNegativo() {
        // Given
        Conta nova = new Conta(
                "Cartao Credito",
                TipoConta.CARTAO_CREDITO,
                new Money(new BigDecimal("-250.00"), BRL)
        );

        // When
        Conta salva = repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(salva.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getSaldoInicial())
                .isEqualTo(new Money(new BigDecimal("-250.00"), BRL));
    }

    @Test
    void salvarPersisteCorretamenteContaCartaoCredito() {
        // Given
        Conta nova = new Conta(
                "Cartao Visa",
                TipoConta.CARTAO_CREDITO,
                new Money(new BigDecimal("0.00"), BRL)
        );

        // When
        repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getTipo()).isEqualTo(TipoConta.CARTAO_CREDITO);
    }

    @Test
    void buscarPorIdRetornaContaQuandoExiste() {
        // Given
        Conta nova = new Conta(
                "Corrente",
                TipoConta.CORRENTE,
                new Money(new BigDecimal("500.00"), BRL)
        );
        repository.salvar(nova);

        // When
        Optional<Conta> resultado = repository.buscarPorId(nova.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(nova.getId());
        assertThat(resultado.get().getNome()).isEqualTo("Corrente");
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        // When
        Optional<Conta> resultado = repository.buscarPorId(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarTodasRetornaContasAtivasEInativas() {
        // Given
        Conta c1 = new Conta("Corrente", TipoConta.CORRENTE, new Money(new BigDecimal("100.00"), BRL));
        Conta c2 = new Conta("Poupanca", TipoConta.POUPANCA, new Money(new BigDecimal("200.00"), BRL));
        Conta c3 = new Conta("Dinheiro", TipoConta.DINHEIRO, new Money(new BigDecimal("50.00"), BRL));
        repository.salvar(c1);
        repository.salvar(c2);
        Conta c3Salva = repository.salvar(c3);
        repository.salvar(c3Salva.desativar());

        // When
        List<Conta> todas = repository.listarTodas();

        // Then
        assertThat(todas).hasSize(3);
    }

    @Test
    void listarAtivasRetornaApenasAtivas() {
        // Given
        Conta c1 = new Conta("Corrente", TipoConta.CORRENTE, new Money(new BigDecimal("100.00"), BRL));
        Conta c2 = new Conta("Poupanca", TipoConta.POUPANCA, new Money(new BigDecimal("200.00"), BRL));
        Conta c3 = new Conta("Dinheiro", TipoConta.DINHEIRO, new Money(new BigDecimal("50.00"), BRL));
        repository.salvar(c1);
        repository.salvar(c2);
        Conta c3Salva = repository.salvar(c3);
        repository.salvar(c3Salva.desativar());

        // When
        List<Conta> ativas = repository.listarAtivas();

        // Then
        assertThat(ativas).hasSize(2);
        assertThat(ativas).allMatch(Conta::isAtiva);
    }

    @Test
    void salvarAplicaDesativacao() throws InterruptedException {
        // Given
        Conta nova = new Conta(
                "Corrente",
                TipoConta.CORRENTE,
                new Money(new BigDecimal("0.00"), BRL)
        );
        Conta salva = repository.salvar(nova);
        Thread.sleep(2);

        // When
        Conta desativada = salva.desativar();
        repository.salvar(desativada);

        // Then
        Optional<Conta> resultado = repository.buscarPorId(nova.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().isAtiva()).isFalse();
        assertThat(resultado.get().getAtualizadoEm().truncatedTo(ChronoUnit.MICROS))
                .isAfter(resultado.get().getCriadoEm().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void salvarComMoedaDiferentePersisteCodigoCorretamente() {
        // Given
        Currency usd = Currency.getInstance("USD");
        Conta nova = new Conta(
                "Conta Dolar",
                TipoConta.CORRENTE,
                new Money(new BigDecimal("1000.00"), usd)
        );

        // When
        repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getSaldoInicial().moeda().getCurrencyCode()).isEqualTo("USD");
    }
}
