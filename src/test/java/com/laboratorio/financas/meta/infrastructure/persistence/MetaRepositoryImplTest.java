package com.laboratorio.financas.meta.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.StatusMeta;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MetaRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate PRAZO_FUTURO = LocalDate.now().plusMonths(6);

    @Autowired
    private MetaRepositoryImpl repository;

    @Autowired
    private MetaJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void salvarPersisteMetaERetornaInstanciaEquivalente() {
        // Given
        Meta nova = new Meta(
                USER_ID,
                "Viagem Europa",
                new Money(new BigDecimal("5000.00"), BRL),
                PRAZO_FUTURO
        );

        // When
        Meta salva = repository.salvar(nova);

        // Then
        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getUserId()).isEqualTo(USER_ID);
        assertThat(salva.getNome()).isEqualTo("Viagem Europa");
        assertThat(salva.getValorAlvo()).isEqualTo(new Money(new BigDecimal("5000.00"), BRL));
        assertThat(salva.getValorAtual()).isEqualTo(new Money(BigDecimal.ZERO, BRL));
        assertThat(salva.getPrazo()).isEqualTo(PRAZO_FUTURO);
        assertThat(salva.getStatus()).isEqualTo(StatusMeta.EM_ANDAMENTO);
    }

    @Test
    void salvarPersisteValorAtualZeroAoCriar() {
        // Given
        Meta nova = new Meta(
                USER_ID,
                "Reserva Emergencia",
                new Money(new BigDecimal("10000.00"), BRL),
                PRAZO_FUTURO
        );

        // When
        repository.salvar(nova);
        Optional<Meta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getValorAtual().valor()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void atualizarPersisteMudancaDeStatus() throws InterruptedException {
        // Given
        Meta nova = new Meta(
                USER_ID,
                "Notebook Novo",
                new Money(new BigDecimal("3000.00"), BRL),
                PRAZO_FUTURO
        );
        repository.salvar(nova);
        Thread.sleep(2);

        // When — deposito suficiente para concluir a meta
        nova.registrarDeposito(new Money(new BigDecimal("3000.00"), BRL));
        repository.atualizar(nova);

        // Then
        Optional<Meta> recuperada = repository.buscarPorId(nova.getId());
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getStatus()).isEqualTo(StatusMeta.CONCLUIDA);
        assertThat(recuperada.get().getValorAtual())
                .isEqualTo(new Money(new BigDecimal("3000.00"), BRL));
    }

    @Test
    void atualizarPersisteCancelamento() {
        // Given
        Meta nova = new Meta(
                USER_ID,
                "Carro Usado",
                new Money(new BigDecimal("25000.00"), BRL),
                PRAZO_FUTURO
        );
        repository.salvar(nova);

        // When
        nova.cancelar();
        repository.atualizar(nova);

        // Then
        Optional<Meta> recuperada = repository.buscarPorId(nova.getId());
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getStatus()).isEqualTo(StatusMeta.CANCELADA);
    }

    @Test
    void atualizarPersisteParcialmenteDepositoSemConcluir() {
        // Given
        Meta nova = new Meta(
                USER_ID,
                "Fundo Aposentadoria",
                new Money(new BigDecimal("100000.00"), BRL),
                PRAZO_FUTURO
        );
        repository.salvar(nova);

        // When — deposito parcial, nao conclui a meta
        nova.registrarDeposito(new Money(new BigDecimal("500.00"), BRL));
        repository.atualizar(nova);

        // Then
        Optional<Meta> recuperada = repository.buscarPorId(nova.getId());
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getStatus()).isEqualTo(StatusMeta.EM_ANDAMENTO);
        assertThat(recuperada.get().getValorAtual())
                .isEqualTo(new Money(new BigDecimal("500.00"), BRL));
    }

    @Test
    void buscarPorIdRetornaMetaQuandoExiste() {
        // Given
        Meta nova = new Meta(
                USER_ID,
                "Intercambio",
                new Money(new BigDecimal("8000.00"), BRL),
                PRAZO_FUTURO
        );
        repository.salvar(nova);

        // When
        Optional<Meta> resultado = repository.buscarPorId(nova.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(nova.getId());
        assertThat(resultado.get().getNome()).isEqualTo("Intercambio");
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        // When
        Optional<Meta> resultado = repository.buscarPorId(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarRetornaTodasAsMetasSalvas() {
        // Given
        Meta m1 = new Meta(USER_ID, "Meta A", new Money(new BigDecimal("1000.00"), BRL), PRAZO_FUTURO);
        Meta m2 = new Meta(USER_ID, "Meta B", new Money(new BigDecimal("2000.00"), BRL), PRAZO_FUTURO);
        Meta m3 = new Meta(USER_ID, "Meta C", new Money(new BigDecimal("3000.00"), BRL), PRAZO_FUTURO);
        repository.salvar(m1);
        repository.salvar(m2);
        repository.salvar(m3);

        // When
        List<Meta> todas = repository.listar();

        // Then
        assertThat(todas).hasSize(3);
        assertThat(todas)
                .extracting(Meta::getId)
                .containsExactlyInAnyOrder(m1.getId(), m2.getId(), m3.getId());
    }

    @Test
    void listarRetornaListaVaziaQuandoNaoHaMetas() {
        // When
        List<Meta> todas = repository.listar();

        // Then
        assertThat(todas).isEmpty();
    }

    @Test
    void listarRetornaMetasComStatusDiferentes() {
        // Given
        Meta ativa = new Meta(USER_ID, "Meta Ativa", new Money(new BigDecimal("1000.00"), BRL), PRAZO_FUTURO);
        Meta cancelada = new Meta(USER_ID, "Meta Cancelada", new Money(new BigDecimal("2000.00"), BRL), PRAZO_FUTURO);
        repository.salvar(ativa);
        repository.salvar(cancelada);
        cancelada.cancelar();
        repository.atualizar(cancelada);

        // When
        List<Meta> todas = repository.listar();

        // Then
        assertThat(todas).hasSize(2);
        assertThat(todas).anyMatch(m -> m.getStatus() == StatusMeta.EM_ANDAMENTO);
        assertThat(todas).anyMatch(m -> m.getStatus() == StatusMeta.CANCELADA);
    }

    @Test
    void salvarPersisteMetaComMoedaUSD() {
        // Given
        Currency usd = Currency.getInstance("USD");
        Meta nova = new Meta(
                USER_ID,
                "Curso no Exterior",
                new Money(new BigDecimal("2000.00"), usd),
                PRAZO_FUTURO
        );

        // When
        repository.salvar(nova);
        Optional<Meta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getValorAlvo().moeda().getCurrencyCode()).isEqualTo("USD");
        assertThat(recuperada.get().getValorAtual().moeda().getCurrencyCode()).isEqualTo("USD");
    }
}
