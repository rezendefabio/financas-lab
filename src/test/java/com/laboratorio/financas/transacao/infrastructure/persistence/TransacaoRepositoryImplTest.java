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
import com.laboratorio.financas.transacao.domain.DirecaoOrdenacao;
import com.laboratorio.financas.transacao.domain.FiltroGenerico;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.OrdenacaoTransacao;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userIdPadrao;

    @org.junit.jupiter.api.BeforeEach
    void preparar() {
        userIdPadrao = criarUsuarioPersistido();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
        categoriaJpaRepository.deleteAll();
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

    private UUID criarContaPersistida() {
        Conta conta = new Conta(
                UUID.randomUUID(),
                userIdPadrao,
                "Conta de Teste",
                TipoConta.CORRENTE,
                new Money(BigDecimal.ZERO, BRL),
                new Money(BigDecimal.ZERO, BRL),
                null,
                null,
                null,
                true,
                Instant.now(),
                null
        );
        contaRepositoryImpl.salvar(conta);
        return conta.getId();
    }

    private UUID criarCategoriaPersistida(TipoCategoria tipo) {
        Categoria cat = new Categoria("Categoria " + tipo, tipo);
        categoriaRepositoryImpl.salvar(cat);
        return cat.getId();
    }

    private Transacao novaReceita(UUID contaId, UUID categoriaId) {
        return new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", contaId,
                categoriaId, null, StatusTransacao.CLEARED, null, List.of()
        );
    }

    private Transacao novaDespesa(UUID contaId) {
        return new Transacao(
                TipoTransacao.DESPESA, VALOR_100, HOJE, "Mercado", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()
        );
    }

    @Test
    void salvarERecuperarReceitaPreservaTodosOsCampos() {
        // Given
        UUID contaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);
        Transacao nova = novaReceita(contaId, categoriaId);

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
        assertThat(t.getCategoriaId()).isEqualTo(categoriaId);
        assertThat(t.getStatus()).isEqualTo(StatusTransacao.CLEARED);
        assertThat(t.isDeleted()).isFalse();
    }

    @Test
    void salvarERecuperarDespesaComDecimalExato() {
        // Given
        UUID contaId = criarContaPersistida();
        Money valorDecimal = new Money(new BigDecimal("49.99"), BRL);
        Transacao nova = new Transacao(
                TipoTransacao.DESPESA, valorDecimal, HOJE, "Mercado", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()
        );

        // When
        repository.salvar(nova);
        Optional<Transacao> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getValor()).isEqualTo(valorDecimal);
    }

    @Test
    void salvarERecuperarLocalDateSemTimezoneShift() {
        // Given
        UUID contaId = criarContaPersistida();
        LocalDate dataEspecifica = LocalDate.of(2026, 1, 15);
        Transacao nova = new Transacao(
                TipoTransacao.RECEITA, VALOR_100, dataEspecifica, "Bonus", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()
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
                TipoTransacao.RECEITA, VALOR_100, HOJE, descricao200, contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()
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
        Transacao nova = novaReceita(contaId, null);
        repository.salvar(nova);

        // When
        repository.deletar(nova.getId());

        // Then
        assertThat(jpaRepository.findById(nova.getId())).isEmpty();
    }

    @Test
    void deletarIdInexistenteNaoLancaExcecao() {
        // Spring Data deleteById ignora silenciosamente id inexistente
        repository.deletar(UUID.randomUUID());
        // se chegar aqui sem excecao, o comportamento esta correto
    }

    // --- Soft delete ---

    @Test
    void softDeleteMarcaDeletedAtETransacaoSumeDaBusca() {
        // Given
        UUID contaId = criarContaPersistida();
        Transacao nova = novaReceita(contaId, null);
        repository.salvar(nova);

        // When
        repository.softDelete(nova.getId());

        // Then -- buscarPorId filtra deleted_at IS NULL
        assertThat(repository.buscarPorId(nova.getId())).isEmpty();
        // Registro ainda existe fisicamente no banco
        assertThat(jpaRepository.findById(nova.getId())).isPresent();
        assertThat(jpaRepository.findById(nova.getId()).get().getDeletedAt()).isNotNull();
    }

    @Test
    void softDeleteNaoAparecaNaListagem() {
        // Given
        UUID contaId = criarContaPersistida();
        Transacao t1 = novaReceita(contaId, null);
        Transacao t2 = novaDespesa(contaId);
        repository.salvar(t1);
        repository.salvar(t2);

        // When
        repository.softDelete(t1.getId());

        // Then -- listagem deve excluir deletadas
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(t2.getId());
    }

    // --- Par de transferencia ---

    @Test
    void salvarParTransferenciaERecuperarPorGroupId() {
        // Given
        UUID contaOrigemId = criarContaPersistida();
        UUID contaDestinoId = criarContaPersistida();

        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                null, VALOR_100, contaOrigemId, contaDestinoId, HOJE, "TED", null
        );
        repository.salvar(par.despesa());
        repository.salvar(par.receita());

        // When
        List<Transacao> grupo = repository.findByTransferGroupId(par.despesa().getTransferGroupId());

        // Then
        assertThat(grupo).hasSize(2);
        assertThat(grupo.stream().map(Transacao::getId))
                .containsExactlyInAnyOrder(par.despesa().getId(), par.receita().getId());
    }

    @Test
    void parTransferenciaTransferPairIdCruzado() {
        // Given
        UUID contaOrigemId = criarContaPersistida();
        UUID contaDestinoId = criarContaPersistida();

        Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                null, VALOR_100, contaOrigemId, contaDestinoId, HOJE, "TED", null
        );
        repository.salvar(par.despesa());
        repository.salvar(par.receita());

        // When
        Optional<Transacao> despesaRecuperada = repository.buscarPorId(par.despesa().getId());
        Optional<Transacao> receitaRecuperada = repository.buscarPorId(par.receita().getId());

        // Then
        assertThat(despesaRecuperada).isPresent();
        assertThat(receitaRecuperada).isPresent();
        assertThat(despesaRecuperada.get().getTransferPairId()).isEqualTo(par.receita().getId());
        assertThat(receitaRecuperada.get().getTransferPairId()).isEqualTo(par.despesa().getId());
    }

    @Test
    void salvarViolaFkQuandoContaIdNaoExiste() {
        // Given -- TransacaoEntity criada diretamente, bypassando domain validation
        UUID contaInexistente = UUID.randomUUID();
        TransacaoEntity entityInvalida = new TransacaoEntity(
                UUID.randomUUID(),
                TipoTransacao.RECEITA,
                new MoneyEmbeddable(new BigDecimal("50.00"), "BRL"),
                HOJE,
                "Teste FK",
                contaInexistente,
                null,
                Instant.now(),
                Instant.now(),
                null,
                StatusTransacao.CLEARED,
                null,
                null,
                null,
                null,
                new HashSet<>()
        );

        // Then -- banco rejeita violacao de FK (defesa em profundidade)
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(entityInvalida))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void salvarViolaCheckConstraintQuandoValorNegativo() {
        // Given -- TransacaoEntity criada diretamente com valor negativo, bypassando domain validation
        UUID contaId = criarContaPersistida();
        TransacaoEntity entityInvalida = new TransacaoEntity(
                UUID.randomUUID(),
                TipoTransacao.RECEITA,
                new MoneyEmbeddable(new BigDecimal("-50.00"), "BRL"),
                HOJE,
                "Teste CHECK",
                contaId,
                null,
                Instant.now(),
                Instant.now(),
                null,
                StatusTransacao.CLEARED,
                null,
                null,
                null,
                null,
                new HashSet<>()
        );

        // Then -- banco rejeita violacao de CHECK constraint (defesa em profundidade)
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(entityInvalida))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- calcularTotaisPorConta ---

    @Test
    void calcularTotaisRetornaReceitasDespesasCorretasParaConta() {
        // Given
        UUID contaId = criarContaPersistida();
        UUID categoriaReceita = criarCategoriaPersistida(TipoCategoria.RECEITA);
        UUID categoriaDespesa = criarCategoriaPersistida(TipoCategoria.DESPESA);

        repository.salvar(new Transacao(
                TipoTransacao.RECEITA, new Money(new BigDecimal("500.00"), BRL), HOJE, "Salario", contaId,
                categoriaReceita, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(
                TipoTransacao.RECEITA, new Money(new BigDecimal("200.00"), BRL), HOJE, "Freelance", contaId,
                categoriaReceita, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(
                TipoTransacao.DESPESA, new Money(new BigDecimal("150.00"), BRL), HOJE, "Aluguel", contaId,
                categoriaDespesa, null, StatusTransacao.CLEARED, null, List.of()));

        // When
        TotaisTransacaoPorConta totais = repository.calcularTotaisPorConta(contaId);

        // Then
        assertThat(totais.totalReceitas()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(totais.totalDespesas()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(totais.totalTransferenciasEnviadas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totais.totalTransferenciasRecebidas()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calcularTotaisRetornaZerosParaContaSemTransacoes() {
        // Given
        UUID contaId = criarContaPersistida();

        // When
        TotaisTransacaoPorConta totais = repository.calcularTotaisPorConta(contaId);

        // Then
        assertThat(totais).isNotNull();
        assertThat(totais.totalReceitas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totais.totalDespesas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totais.totalTransferenciasEnviadas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totais.totalTransferenciasRecebidas()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calcularTotaisNaoMisturaDadosDeContasDiferentes() {
        // Given
        UUID contaAlvoId = criarContaPersistida();
        UUID outraContaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        repository.salvar(new Transacao(
                TipoTransacao.RECEITA, new Money(new BigDecimal("1000.00"), BRL), HOJE, "Receita alvo", contaAlvoId,
                categoriaId, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(
                TipoTransacao.RECEITA, new Money(new BigDecimal("9999.00"), BRL), HOJE, "Receita outra", outraContaId,
                categoriaId, null, StatusTransacao.CLEARED, null, List.of()));

        // When
        TotaisTransacaoPorConta totais = repository.calcularTotaisPorConta(contaAlvoId);

        // Then
        assertThat(totais.totalReceitas()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void calcularTotaisNaoContaTrasacoesDeletedasSoftDeleted() {
        // Given
        UUID contaId = criarContaPersistida();
        Transacao nova = novaReceita(contaId, null);
        repository.salvar(nova);
        repository.softDelete(nova.getId());

        // When
        TotaisTransacaoPorConta totais = repository.calcularTotaisPorConta(contaId);

        // Then
        assertThat(totais.totalReceitas()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- listarComFiltros ---

    @Test
    void listarComFiltrosRetornaTodosQuandoFiltrosNulos() {
        // Given
        UUID contaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        repository.salvar(novaReceita(contaId, categoriaId));
        repository.salvar(novaDespesa(contaId));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void listarComFiltrosPorContaRetornaApenasTransacoesDaquelaConta() {
        // Given
        UUID contaAlvoId = criarContaPersistida();
        UUID outraContaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        repository.salvar(new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Alvo", contaAlvoId,
                categoriaId, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(
                TipoTransacao.RECEITA, VALOR_100, HOJE, "Outra", outraContaId,
                categoriaId, null, StatusTransacao.CLEARED, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(contaAlvoId, null, null, null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Alvo");
    }

    @Test
    void listarComFiltrosPorTipoRetornaApenasDoTipoFiltrado() {
        // Given
        UUID contaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        repository.salvar(novaReceita(contaId, categoriaId));
        repository.salvar(novaDespesa(contaId));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, TipoTransacao.RECEITA, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getTipo()).isEqualTo(TipoTransacao.RECEITA);
    }

    @Test
    void listarComFiltrosPorIntervaloDeDataRetornaApenasNoPeriodo() {
        // Given
        UUID contaId = criarContaPersistida();
        LocalDate dataPassada = LocalDate.of(2026, 1, 10);
        LocalDate dataRecente = LocalDate.of(2026, 5, 10);
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, dataPassada, "Janeiro", contaId,
                categoriaId, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, dataRecente, "Maio", contaId,
                categoriaId, null, StatusTransacao.CLEARED, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Maio");
    }

    @Test
    void listarComFiltrosPorCategoriaRetornaApenasComAquelaCategoria() {
        // Given
        UUID contaId = criarContaPersistida();
        UUID categoriaAlvoId = criarCategoriaPersistida(TipoCategoria.RECEITA);
        UUID outraCategoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Com categoria alvo", contaId,
                categoriaAlvoId, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Com outra categoria", contaId,
                outraCategoriaId, null, StatusTransacao.CLEARED, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, categoriaAlvoId);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getCategoriaId()).isEqualTo(categoriaAlvoId);
    }

    @Test
    void listarComFiltrosPaginacaoRetornaSubconjuntoCorreto() {
        // Given
        UUID contaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        for (int i = 1; i <= 5; i++) {
            repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "T" + i, contaId,
                    categoriaId, null, StatusTransacao.CLEARED, null, List.of()));
        }

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        // When
        Page<Transacao> pagina0 = repository.listarComFiltros(filtros, PageRequest.of(0, 2));
        Page<Transacao> pagina1 = repository.listarComFiltros(filtros, PageRequest.of(1, 2));

        // Then
        assertThat(pagina0.getTotalElements()).isEqualTo(5);
        assertThat(pagina0.getContent()).hasSize(2);
        assertThat(pagina1.getContent()).hasSize(2);
        assertThat(pagina0.getTotalPages()).isEqualTo(3);
    }

    @Test
    void listarComFiltroPorStatusRetornaApenasComAqueleStatus() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Compensada", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Pendente", contaId,
                null, null, StatusTransacao.PENDING, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Agendada", contaId,
                null, null, StatusTransacao.SCHEDULED, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null, null,
                StatusTransacao.PENDING);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getStatus()).isEqualTo(StatusTransacao.PENDING);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Pendente");
    }

    @Test
    void listarComStatusNuloNaoFiltraPorStatus() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Compensada", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Pendente", contaId,
                null, null, StatusTransacao.PENDING, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null, null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void listarComFiltroStatusCombinadoComTipo() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Receita pendente", contaId,
                null, null, StatusTransacao.PENDING, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Despesa pendente", contaId,
                null, null, StatusTransacao.PENDING, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, TipoTransacao.RECEITA, null, null,
                StatusTransacao.PENDING);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Receita pendente");
    }

    @Test
    void listarNaoRetornaTransacoesSoftDeleted() {
        // Given
        UUID contaId = criarContaPersistida();
        Transacao t1 = novaReceita(contaId, null);
        Transacao t2 = novaDespesa(contaId);
        repository.salvar(t1);
        repository.salvar(t2);
        repository.softDelete(t1.getId());

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    // --- listarComFiltrosOrdenado ---

    @Test
    void listarComFiltrosOrdenadoPorValorAscTraduzPathDoEmbeddable() {
        // Given -- VALOR e um @Embedded MoneyEmbeddable; a infra deve traduzir
        // OrdenacaoTransacao.VALOR para o path JPA valor.valor.
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, new Money(new BigDecimal("900.00"), BRL),
                HOJE, "Maior", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, new Money(new BigDecimal("10.00"), BRL),
                HOJE, "Menor", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltrosOrdenado(
                filtros, 0, 10, OrdenacaoTransacao.VALOR, DirecaoOrdenacao.ASC);

        // Then
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Menor");
        assertThat(resultado.getContent().get(1).getDescricao()).isEqualTo("Maior");
    }

    @Test
    void listarComFiltrosOrdenadoPorValorDescTraduzPathDoEmbeddable() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, new Money(new BigDecimal("900.00"), BRL),
                HOJE, "Maior", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, new Money(new BigDecimal("10.00"), BRL),
                HOJE, "Menor", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltrosOrdenado(
                filtros, 0, 10, OrdenacaoTransacao.VALOR, DirecaoOrdenacao.DESC);

        // Then
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Maior");
        assertThat(resultado.getContent().get(1).getDescricao()).isEqualTo("Menor");
    }

    @Test
    void listarComFiltrosOrdenadoPorDescricaoAsc() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100,
                HOJE, "Beta", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100,
                HOJE, "Alfa", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));

        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        // When
        Page<Transacao> resultado = repository.listarComFiltrosOrdenado(
                filtros, 0, 10, OrdenacaoTransacao.DESCRICAO, DirecaoOrdenacao.ASC);

        // Then
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Alfa");
        assertThat(resultado.getContent().get(1).getDescricao()).isEqualTo("Beta");
    }

    @Test
    void listarComFiltrosOrdenadoRespeitaPaginacao() {
        // Given
        UUID contaId = criarContaPersistida();
        for (int i = 1; i <= 5; i++) {
            repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "T" + i, contaId,
                    null, null, StatusTransacao.CLEARED, null, List.of()));
        }
        FiltrosTransacao filtros = new FiltrosTransacao(null, null, null, null, null);

        // When
        Page<Transacao> pagina0 = repository.listarComFiltrosOrdenado(
                filtros, 0, 2, OrdenacaoTransacao.DATA, DirecaoOrdenacao.DESC);

        // Then
        assertThat(pagina0.getTotalElements()).isEqualTo(5);
        assertThat(pagina0.getContent()).hasSize(2);
        assertThat(pagina0.getTotalPages()).isEqualTo(3);
    }

    // --- Filtros adicionais (FiltroGenerico) ---

    private FiltrosTransacao filtrosAdicionais(FiltroGenerico... adicionais) {
        return new FiltrosTransacao(null, null, null, null, null, null, null, List.of(adicionais));
    }

    @Test
    void listarComFiltroAdicionalContainsEmDescricaoFiltraPorTexto() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Compra no Mercado", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Pagamento Aluguel", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));

        // When -- contains e case-insensitive.
        FiltrosTransacao filtros = filtrosAdicionais(
                new FiltroGenerico("descricao", "contains", "mercado"));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Compra no Mercado");
    }

    @Test
    void listarComFiltroAdicionalNotContainsEmDescricaoExcluiPorTexto() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Compra no Mercado", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Pagamento Aluguel", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));

        // When
        FiltrosTransacao filtros = filtrosAdicionais(
                new FiltroGenerico("descricao", "not_contains", "mercado"));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Pagamento Aluguel");
    }

    @Test
    void listarComFiltroAdicionalGteEmValorFiltraPorValor() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, new Money(new BigDecimal("50.00"), BRL),
                HOJE, "Baixo", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, new Money(new BigDecimal("500.00"), BRL),
                HOJE, "Alto", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));

        // When
        FiltrosTransacao filtros = filtrosAdicionais(
                new FiltroGenerico("valor", "gte", "100"));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Alto");
    }

    @Test
    void listarComFiltroAdicionalIntervaloDeValorGteELte() {
        // Given
        UUID contaId = criarContaPersistida();
        for (String v : new String[] {"50.00", "250.00", "700.00"}) {
            repository.salvar(new Transacao(TipoTransacao.RECEITA, new Money(new BigDecimal(v), BRL),
                    HOJE, "Valor " + v, contaId, null, null, StatusTransacao.CLEARED, null, List.of()));
        }

        // When
        FiltrosTransacao filtros = filtrosAdicionais(
                new FiltroGenerico("valor", "gte", "100"),
                new FiltroGenerico("valor", "lte", "500"));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Valor 250.00");
    }

    @Test
    void listarComFiltroAdicionalGteELteEmDataFiltraPorPeriodo() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, LocalDate.of(2026, 1, 10),
                "Janeiro", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, LocalDate.of(2026, 6, 10),
                "Junho", contaId, null, null, StatusTransacao.CLEARED, null, List.of()));

        // When
        FiltrosTransacao filtros = filtrosAdicionais(
                new FiltroGenerico("data", "gte", "2026-01-01"),
                new FiltroGenerico("data", "lte", "2026-03-31"));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Janeiro");
    }

    @Test
    void listarComFiltroAdicionalEqEmDescricaoComparaTextoExato() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Salario extra", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));

        // When
        FiltrosTransacao filtros = filtrosAdicionais(
                new FiltroGenerico("descricao", "eq", "salario"));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then -- eq compara o texto inteiro (case-insensitive), nao substring.
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Salario");
    }

    @Test
    void listarComFiltroAdicionalCombinaComFiltroFixoDeTipo() {
        // Given
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Mercado receita", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.DESPESA, VALOR_100, HOJE, "Mercado despesa", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));

        // When -- filtro fixo (tipo) + filtro adicional (descricao contains).
        FiltrosTransacao filtros = new FiltrosTransacao(
                null, null, null, TipoTransacao.RECEITA, null, null, null,
                List.of(new FiltroGenerico("descricao", "contains", "mercado")));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Mercado receita");
    }

    @Test
    void listarComFiltroAdicionalContainsTrataCuringaLikeComoLiteral() {
        // Given -- valor com '%' deve ser tratado como literal, nao curinga.
        UUID contaId = criarContaPersistida();
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Desconto 50%", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));
        repository.salvar(new Transacao(TipoTransacao.RECEITA, VALOR_100, HOJE, "Sem desconto", contaId,
                null, null, StatusTransacao.CLEARED, null, List.of()));

        // When
        FiltrosTransacao filtros = filtrosAdicionais(
                new FiltroGenerico("descricao", "contains", "50%"));
        Page<Transacao> resultado = repository.listarComFiltros(filtros, PageRequest.of(0, 10));

        // Then -- so a transacao que literalmente contem "50%".
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getDescricao()).isEqualTo("Desconto 50%");
    }
}
