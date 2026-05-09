package com.laboratorio.financas.transacao.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaJpaRepository;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaRepositoryImpl;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaRepositoryImpl;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class TransacaoRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money VALOR_100 = new Money(new BigDecimal("100.00"), BRL);
    private static final LocalDate HOJE = LocalDate.now();

    @Autowired
    private TransacaoRepositoryImpl repository;

    @Autowired
    private TransacaoJpaRepository jpaRepository;

    @Autowired
    private ContaRepositoryImpl contaRepositoryImpl;

    @Autowired
    private ContaJpaRepository contaJpaRepository;

    @Autowired
    private CategoriaRepositoryImpl categoriaRepositoryImpl;

    @Autowired
    private CategoriaJpaRepository categoriaJpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
        categoriaJpaRepository.deleteAll();
    }

    private UUID criarContaPersistida() {
        Conta conta = new Conta("Conta de Teste", TipoConta.CORRENTE, new Money(BigDecimal.ZERO, BRL));
        contaRepositoryImpl.salvar(conta);
        return conta.getId();
    }

    private UUID criarCategoriaPersistida(TipoCategoria tipo) {
        Categoria cat = new Categoria("Categoria " + tipo, tipo);
        categoriaRepositoryImpl.salvar(cat);
        return cat.getId();
    }

    @Test
    void salvarERecuperarReceitaPreservaTodosOsCampos() {
        // Given
        UUID contaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);
        Transacao nova = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", contaId, null, categoriaId
        );

        // When
        Transacao salva = repository.salvar(nova);
        Optional<Transacao> recuperada = repository.buscarPorId(salva.getId());

        // Then
        assertThat(recuperada).isPresent();
        Transacao t = recuperada.get();
        assertThat(t.getId()).isEqualTo(nova.getId());
        assertThat(t.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(t.getValor()).isEqualTo(VALOR_100);
        assertThat(t.getData()).isEqualTo(HOJE);
        assertThat(t.getDescricao()).isEqualTo("Salario");
        assertThat(t.getContaId()).isEqualTo(contaId);
        assertThat(t.getContaDestinoId()).isNull();
        assertThat(t.getCategoriaId()).isEqualTo(categoriaId);
    }

    @Test
    void salvarERecuperarDespesaComDecimalExato() {
        // Given
        UUID contaId = criarContaPersistida();
        Money valorDecimal = new Money(new BigDecimal("49.99"), BRL);
        Transacao nova = new Transacao(
                TipoTransacao.DESPESA, valorDecimal, HOJE, "Mercado", contaId, null, null
        );

        // When
        repository.salvar(nova);
        Optional<Transacao> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getValor()).isEqualTo(valorDecimal);
    }

    @Test
    void salvarERecuperarTransferenciaComContaDestinoPreservada() {
        // Given
        UUID contaOrigemId = criarContaPersistida();
        UUID contaDestinoId = criarContaPersistida();
        Transacao nova = new Transacao(
                TipoTransacao.TRANSFERENCIA, VALOR_100, HOJE, "TED",
                contaOrigemId, contaDestinoId, null
        );

        // When
        repository.salvar(nova);
        Optional<Transacao> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getContaDestinoId()).isEqualTo(contaDestinoId);
        assertThat(recuperada.get().getCategoriaId()).isNull();
    }

    @Test
    void salvarERecuperarLocalDateSemTimezoneShift() {
        // Given
        UUID contaId = criarContaPersistida();
        LocalDate dataEspecifica = LocalDate.of(2026, 1, 15);
        Transacao nova = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, dataEspecifica, "Bonus", contaId, null, null
        );

        // When
        repository.salvar(nova);
        Optional<Transacao> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getData()).isEqualTo(dataEspecifica);
    }

    @Test
    void salvarERecuperarDescricaoComExatamente200Chars() {
        // Given
        UUID contaId = criarContaPersistida();
        String descricao200 = "X".repeat(200);
        Transacao nova = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, descricao200, contaId, null, null
        );

        // When
        repository.salvar(nova);
        Optional<Transacao> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getDescricao()).hasSize(200);
    }

    @Test
    void buscarPorIdRetornaVazioQuandoInexistente() {
        // When
        Optional<Transacao> resultado = repository.buscarPorId(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void deletarRemoveDosBanco() {
        // Given
        UUID contaId = criarContaPersistida();
        Transacao nova = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", contaId, null, null
        );
        repository.salvar(nova);

        // When
        repository.deletar(nova.getId());

        // Then
        assertThat(repository.buscarPorId(nova.getId())).isEmpty();
    }

    @Test
    void deletarIdInexistenteNaoLancaExcecao() {
        // Spring Data deleteById ignora silenciosamente id inexistente
        repository.deletar(UUID.randomUUID());
        // se chegar aqui sem excecao, o comportamento esta correto
    }

    @Test
    void salvarViolaFkQuandoContaIdNaoExiste() {
        // Given — TransacaoEntity criada diretamente, bypassando domain validation
        UUID contaInexistente = UUID.randomUUID();
        TransacaoEntity entityInvalida = new TransacaoEntity(
                UUID.randomUUID(),
                TipoTransacao.RECEITA,
                new MoneyEmbeddable(new BigDecimal("50.00"), "BRL"),
                HOJE,
                "Teste FK",
                contaInexistente,
                null,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        // Then — banco rejeita violacao de FK (defesa em profundidade)
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(entityInvalida))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void salvarViolaCheckConstraintQuandoValorNegativo() {
        // Given — TransacaoEntity criada diretamente com valor negativo, bypassando domain validation
        UUID contaId = criarContaPersistida();
        TransacaoEntity entityInvalida = new TransacaoEntity(
                UUID.randomUUID(),
                TipoTransacao.RECEITA,
                new MoneyEmbeddable(new BigDecimal("-50.00"), "BRL"),
                HOJE,
                "Teste CHECK",
                contaId,
                null,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        // Then — banco rejeita violacao de CHECK constraint (defesa em profundidade)
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(entityInvalida))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
