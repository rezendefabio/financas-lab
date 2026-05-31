package com.laboratorio.financas.conta.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
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

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    private UUID criarUsuarioPersistido() {
        UUID id = UUID.randomUUID();
        UsuarioEntity entity = new UsuarioEntity(
                id,
                "teste+" + id + "@test.com",
                "hash_bcrypt",
                true,
                Instant.now(),
                null,
                Instant.now()
        );
        usuarioJpaRepository.save(entity);
        return id;
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

    @Test
    void salvarComUserIdPersisteCorretamente() {
        // Given
        UUID userId = criarUsuarioPersistido();
        Conta nova = new Conta(
                UUID.randomUUID(),
                userId,
                "Conta Com User",
                TipoConta.CORRENTE,
                new Money(new BigDecimal("0.00"), BRL),
                new Money(new BigDecimal("0.00"), BRL),
                null,
                null,
                null,
                true,
                Instant.now(),
                null
        );

        // When
        repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void salvarComCamposCartaoCreditoPersisteCorretamente() {
        // Given
        Money limiteCredito = new Money(new BigDecimal("5000.00"), BRL);
        Conta nova = new Conta(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cartao Visa",
                TipoConta.CARTAO_CREDITO,
                new Money(new BigDecimal("0.00"), BRL),
                new Money(new BigDecimal("0.00"), BRL),
                limiteCredito,
                10,
                20,
                true,
                Instant.now(),
                null
        );

        // When
        repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getLimiteCredito()).isEqualTo(limiteCredito);
        assertThat(recuperada.get().getDiaFechamento()).isEqualTo(10);
        assertThat(recuperada.get().getDiaVencimento()).isEqualTo(20);
    }

    @Test
    void salvarComSaldoAtualDiferenteDeSaldoInicialPersisteCorretamente() {
        // Given
        Money saldoInicial = new Money(new BigDecimal("1000.00"), BRL);
        Money saldoAtual = new Money(new BigDecimal("1500.00"), BRL);
        Conta nova = new Conta(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Conta Atualizada",
                TipoConta.CORRENTE,
                saldoInicial,
                saldoAtual,
                null,
                null,
                null,
                true,
                Instant.now(),
                null
        );

        // When
        repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getSaldoAtual()).isEqualTo(saldoAtual);
    }

    @Test
    void salvarComTipoInvestimentoPersisteCorretamente() {
        // Given
        Conta nova = new Conta(
                "CDB Nubank",
                TipoConta.INVESTIMENTO,
                new Money(new BigDecimal("2000.00"), BRL)
        );

        // When
        repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getTipo()).isEqualTo(TipoConta.INVESTIMENTO);
    }

    @Test
    void salvarComTipoOutroPersisteCorretamente() {
        // Given
        Conta nova = new Conta(
                "Conta Outro",
                TipoConta.OUTRO,
                new Money(new BigDecimal("100.00"), BRL)
        );

        // When
        repository.salvar(nova);
        Optional<Conta> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getTipo()).isEqualTo(TipoConta.OUTRO);
    }

    @Test
    void deletarContaExistenteRemoveDoRepositorio() {
        // Given
        Conta nova = new Conta(
                "Conta Para Deletar",
                TipoConta.CORRENTE,
                new Money(new BigDecimal("100.00"), BRL)
        );
        Conta salva = repository.salvar(nova);

        // When
        repository.deletar(salva.getId());

        // Then
        Optional<Conta> resultado = repository.buscarPorId(salva.getId());
        assertThat(resultado).isEmpty();
    }

    @Test
    void deletarContaInexistenteNaoLancaExcecao() {
        // Given
        UUID idInexistente = UUID.randomUUID();

        // When / Then -- deleteById no Spring Data nao lanca excecao se id nao existe
        repository.deletar(idInexistente);
        // Se chegou aqui sem excecao, o comportamento esta correto
    }
}
