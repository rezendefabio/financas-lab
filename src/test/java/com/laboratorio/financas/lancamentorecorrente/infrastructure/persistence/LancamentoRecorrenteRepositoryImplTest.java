package com.laboratorio.financas.lancamentorecorrente.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaRepositoryImpl;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LancamentoRecorrenteRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money VALOR_500 = new Money(new BigDecimal("500.00"), BRL);
    private static final LocalDate PROXIMA = LocalDate.of(2026, 7, 1);

    @Autowired
    private LancamentoRecorrenteRepositoryImpl repository;

    @Autowired
    private LancamentoRecorrenteJpaRepository jpaRepository;

    @Autowired
    private ContaRepositoryImpl contaRepository;

    @Autowired
    private ContaJpaRepository contaJpaRepository;

    private UUID contaId;

    @BeforeEach
    void criarContaParaTestes() {
        Conta conta = new Conta("Conta Teste", TipoConta.CORRENTE,
                new Money(BigDecimal.ZERO, BRL));
        contaRepository.salvar(conta);
        contaId = conta.getId();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
    }

    private LancamentoRecorrente novoLancamento() {
        return new LancamentoRecorrente(
                "Aluguel", TipoTransacao.DESPESA, VALOR_500,
                contaId, null, Periodicidade.MENSAL, PROXIMA);
    }

    @Test
    void salvarPersisteERetornaEquivalente() {
        LancamentoRecorrente novo = novoLancamento();

        LancamentoRecorrente salvo = repository.salvar(novo);

        assertThat(salvo.getId()).isEqualTo(novo.getId());
        assertThat(salvo.getDescricao()).isEqualTo("Aluguel");
        assertThat(salvo.getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(salvo.getValor()).isEqualTo(VALOR_500);
        assertThat(salvo.getPeriodicidade()).isEqualTo(Periodicidade.MENSAL);
        assertThat(salvo.getProximaOcorrencia()).isEqualTo(PROXIMA);
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void buscarPorIdRetornaLancamentoQuandoExiste() {
        LancamentoRecorrente novo = novoLancamento();
        repository.salvar(novo);

        Optional<LancamentoRecorrente> resultado = repository.buscarPorId(novo.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(novo.getId());
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        Optional<LancamentoRecorrente> resultado = repository.buscarPorId(UUID.randomUUID());

        assertThat(resultado).isEmpty();
    }

    @Test
    void listarRetornaTodosOsLancamentos() {
        LancamentoRecorrente l1 = new LancamentoRecorrente(
                "Streaming", TipoTransacao.DESPESA,
                new Money(new BigDecimal("40.00"), BRL),
                contaId, null, Periodicidade.MENSAL, PROXIMA);
        LancamentoRecorrente l2 = new LancamentoRecorrente(
                "Academia", TipoTransacao.DESPESA,
                new Money(new BigDecimal("100.00"), BRL),
                contaId, null, Periodicidade.MENSAL, PROXIMA);
        repository.salvar(l1);
        repository.salvar(l2);

        List<LancamentoRecorrente> todos = repository.listar();

        assertThat(todos).hasSize(2);
        assertThat(todos).extracting(LancamentoRecorrente::getId)
                .containsExactlyInAnyOrder(l1.getId(), l2.getId());
    }

    @Test
    void listarRetornaListaVaziaQuandoNaoHaLancamentos() {
        List<LancamentoRecorrente> todos = repository.listar();

        assertThat(todos).isEmpty();
    }

    @Test
    void atualizarPersisteMudancaDeAtivo() {
        LancamentoRecorrente novo = novoLancamento();
        repository.salvar(novo);

        novo.desativar();
        repository.atualizar(novo);

        Optional<LancamentoRecorrente> recuperado = repository.buscarPorId(novo.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().isAtivo()).isFalse();
    }

    @Test
    void atualizarPersisteMudancaDeProximaOcorrencia() {
        LancamentoRecorrente novo = novoLancamento();
        repository.salvar(novo);

        novo.avancarProximaOcorrencia();
        repository.atualizar(novo);

        Optional<LancamentoRecorrente> recuperado = repository.buscarPorId(novo.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getProximaOcorrencia()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void salvarPersisteComCategoriaIdNulo() {
        LancamentoRecorrente semCategoria = new LancamentoRecorrente(
                "Supermercado", TipoTransacao.DESPESA, VALOR_500,
                contaId, null, Periodicidade.SEMANAL, PROXIMA);
        repository.salvar(semCategoria);

        Optional<LancamentoRecorrente> recuperado = repository.buscarPorId(semCategoria.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getCategoriaId()).isNull();
    }
}
