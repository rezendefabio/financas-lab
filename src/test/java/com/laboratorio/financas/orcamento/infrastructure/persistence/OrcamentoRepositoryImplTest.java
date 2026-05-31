package com.laboratorio.financas.orcamento.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaJpaRepository;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaRepositoryImpl;
import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrcamentoRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Money LIMITE_500 = new Money(new BigDecimal("500.00"), BRL);
    private static final LocalDate MES_ATUAL = LocalDate.now().withDayOfMonth(1);

    @Autowired
    private OrcamentoRepositoryImpl repository;

    @Autowired
    private OrcamentoJpaRepository jpaRepository;

    @Autowired
    private CategoriaRepositoryImpl categoriaRepositoryImpl;

    @Autowired
    private CategoriaJpaRepository categoriaJpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        categoriaJpaRepository.deleteAll();
    }

    private UUID criarCategoriaPersistida() {
        Categoria cat = new Categoria("Alimentacao", TipoCategoria.DESPESA);
        categoriaRepositoryImpl.salvar(cat);
        return cat.getId();
    }

    @Test
    void salvarPersisteOrcamentoERetornaInstanciaEquivalente() {
        // Given
        UUID categoriaId = criarCategoriaPersistida();
        Orcamento novo = new Orcamento(USER_ID, categoriaId, LIMITE_500, MES_ATUAL);

        // When
        Orcamento salvo = repository.salvar(novo);

        // Then
        assertThat(salvo.getId()).isEqualTo(novo.getId());
        assertThat(salvo.getUserId()).isEqualTo(USER_ID);
        assertThat(salvo.getCategoriaId()).isEqualTo(categoriaId);
        assertThat(salvo.getValorLimite()).isEqualTo(LIMITE_500);
        assertThat(salvo.getMesAno()).isEqualTo(MES_ATUAL);
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void salvarNormalizaMesAnoParaPrimeiroDia() {
        // Given — dia 15 deve ser normalizado para dia 1
        UUID categoriaId = criarCategoriaPersistida();
        LocalDate dia15 = LocalDate.now().withDayOfMonth(15);
        Orcamento novo = new Orcamento(USER_ID, categoriaId, LIMITE_500, dia15);

        // When
        repository.salvar(novo);
        Optional<Orcamento> recuperado = repository.buscarPorId(novo.getId());

        // Then
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getMesAno().getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void salvarPersisteValorLimiteComDuasCasasDecimais() {
        // Given
        UUID categoriaId = criarCategoriaPersistida();
        Money limiteDecimal = new Money(new BigDecimal("1250.75"), BRL);
        Orcamento novo = new Orcamento(USER_ID, categoriaId, limiteDecimal, MES_ATUAL);

        // When
        repository.salvar(novo);
        Optional<Orcamento> recuperado = repository.buscarPorId(novo.getId());

        // Then
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getValorLimite()).isEqualTo(limiteDecimal);
    }

    @Test
    void buscarPorIdRetornaOrcamentoQuandoExiste() {
        // Given
        UUID categoriaId = criarCategoriaPersistida();
        Orcamento novo = new Orcamento(USER_ID, categoriaId, LIMITE_500, MES_ATUAL);
        repository.salvar(novo);

        // When
        Optional<Orcamento> resultado = repository.buscarPorId(novo.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(novo.getId());
        assertThat(resultado.get().getCategoriaId()).isEqualTo(categoriaId);
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        // When
        Optional<Orcamento> resultado = repository.buscarPorId(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarRetornaOrcamentosAtivosEInativos() {
        // Given
        UUID categoriaId = criarCategoriaPersistida();
        Orcamento o1 = new Orcamento(USER_ID, categoriaId, LIMITE_500, MES_ATUAL);
        Orcamento o2 = new Orcamento(USER_ID, categoriaId, new Money(new BigDecimal("200.00"), BRL), LocalDate.of(2026, 3, 1));
        repository.salvar(o1);
        Orcamento o2Salvo = repository.salvar(o2);

        // desativa o2 via atualizar
        o2Salvo.desativar();
        repository.atualizar(o2Salvo);

        // When
        List<Orcamento> todos = repository.listar();

        // Then
        assertThat(todos).hasSize(2);
    }

    @Test
    void listarRetornaListaVaziaQuandoNenhumOrcamento() {
        // When
        List<Orcamento> todos = repository.listar();

        // Then
        assertThat(todos).isEmpty();
    }

    @Test
    void atualizarPersisteMudancaDeAtivo() throws InterruptedException {
        // Given
        UUID categoriaId = criarCategoriaPersistida();
        Orcamento novo = new Orcamento(USER_ID, categoriaId, LIMITE_500, MES_ATUAL);
        repository.salvar(novo);
        Thread.sleep(2);

        // When
        novo.desativar();
        repository.atualizar(novo);

        // Then
        Optional<Orcamento> resultado = repository.buscarPorId(novo.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().isAtivo()).isFalse();
        assertThat(resultado.get().getAtualizadoEm().truncatedTo(ChronoUnit.MICROS))
                .isAfter(resultado.get().getCriadoEm().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void atualizarRetornaInstanciaComDadosPersistidos() {
        // Given
        UUID categoriaId = criarCategoriaPersistida();
        Orcamento novo = new Orcamento(USER_ID, categoriaId, LIMITE_500, MES_ATUAL);
        repository.salvar(novo);

        // When
        novo.desativar();
        Orcamento atualizado = repository.atualizar(novo);

        // Then
        assertThat(atualizado.getId()).isEqualTo(novo.getId());
        assertThat(atualizado.isAtivo()).isFalse();
    }

    @Test
    void salvarComMoedaDiferentePersisteCodigoCorretamente() {
        // Given
        UUID categoriaId = criarCategoriaPersistida();
        Currency usd = Currency.getInstance("USD");
        Money limiteUsd = new Money(new BigDecimal("300.00"), usd);
        Orcamento novo = new Orcamento(USER_ID, categoriaId, limiteUsd, MES_ATUAL);

        // When
        repository.salvar(novo);
        Optional<Orcamento> recuperado = repository.buscarPorId(novo.getId());

        // Then
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getValorLimite().moeda().getCurrencyCode()).isEqualTo("USD");
    }
}
