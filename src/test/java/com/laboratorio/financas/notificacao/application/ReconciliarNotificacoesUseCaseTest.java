package com.laboratorio.financas.notificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaRepository;
import com.laboratorio.financas.meta.domain.StatusMeta;
import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.NotificacaoRepository;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import com.laboratorio.financas.orcamento.application.CalcularProgressoDoOrcamentoUseCase;
import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import com.laboratorio.financas.orcamento.domain.StatusProgresso;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ReconciliarNotificacoesUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private NotificacaoRepository notificacaoRepository;
    private OrcamentoRepository orcamentoRepository;
    private CalcularProgressoDoOrcamentoUseCase calcularProgresso;
    private MetaRepository metaRepository;
    private CategoriaRepository categoriaRepository;
    private ReconciliarNotificacoesUseCase useCase;

    @BeforeEach
    void setUp() {
        notificacaoRepository = Mockito.mock(NotificacaoRepository.class);
        orcamentoRepository = Mockito.mock(OrcamentoRepository.class);
        calcularProgresso = Mockito.mock(CalcularProgressoDoOrcamentoUseCase.class);
        metaRepository = Mockito.mock(MetaRepository.class);
        categoriaRepository = Mockito.mock(CategoriaRepository.class);
        useCase = new ReconciliarNotificacoesUseCase(
                notificacaoRepository, orcamentoRepository, calcularProgresso,
                metaRepository, categoriaRepository);

        when(orcamentoRepository.listar()).thenReturn(List.of());
        when(metaRepository.listar()).thenReturn(List.of());
        when(notificacaoRepository.listarPorUserId(USER_ID)).thenReturn(List.of());
        when(notificacaoRepository.buscarPorChaveNatural(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private Orcamento orcamento(UUID id, UUID categoriaId, boolean ativo) {
        return new Orcamento(id, categoriaId, new Money(new BigDecimal("500"), BRL),
                LocalDate.now(), ativo, Instant.now(), Instant.now());
    }

    private CalcularProgressoDoOrcamentoUseCase.Resultado progresso(
            UUID orcamentoId, StatusProgresso status, String percentual) {
        return new CalcularProgressoDoOrcamentoUseCase.Resultado(
                orcamentoId, UUID.randomUUID(), LocalDate.now(),
                new Money(new BigDecimal("500"), BRL),
                new Money(new BigDecimal("600"), BRL),
                new BigDecimal(percentual), status);
    }

    private Categoria categoria(String nome) {
        return new Categoria(nome, TipoCategoria.DESPESA);
    }

    private Meta meta(UUID id, String nome, LocalDate prazo, StatusMeta status) {
        return new Meta(id, nome, new Money(new BigDecimal("1000"), BRL),
                new Money(BigDecimal.ZERO, BRL), prazo, status, Instant.now(), Instant.now());
    }

    @Test
    void orcamentoExcedidoSemNotificacaoPersistidaCriaNova() {
        UUID orcId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        when(orcamentoRepository.listar()).thenReturn(List.of(orcamento(orcId, catId, true)));
        when(calcularProgresso.executar(orcId))
                .thenReturn(progresso(orcId, StatusProgresso.EXCEDIDO, "120"));
        when(categoriaRepository.buscarPorId(catId)).thenReturn(Optional.of(categoria("Alimentacao")));

        useCase.executar(USER_ID);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).salvar(captor.capture());
        Notificacao salva = captor.getValue();
        assertThat(salva.getTipo()).isEqualTo(TipoNotificacao.ORCAMENTO_EXCEDIDO);
        assertThat(salva.getReferenciaId()).isEqualTo(orcId);
        assertThat(salva.getDescricao()).isEqualTo("Alimentacao: 120% utilizado");
    }

    @Test
    void orcamentoExcedidoComNotificacaoExistenteAtualizaEmVezDeCriar() {
        UUID orcId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        when(orcamentoRepository.listar()).thenReturn(List.of(orcamento(orcId, catId, true)));
        when(calcularProgresso.executar(orcId))
                .thenReturn(progresso(orcId, StatusProgresso.EXCEDIDO, "130"));
        when(categoriaRepository.buscarPorId(catId)).thenReturn(Optional.of(categoria("Lazer")));

        Notificacao existente = new Notificacao(USER_ID, TipoNotificacao.ORCAMENTO_EXCEDIDO, orcId,
                "Orcamento excedido", "Lazer: 120% utilizado");
        when(notificacaoRepository.buscarPorChaveNatural(USER_ID, TipoNotificacao.ORCAMENTO_EXCEDIDO, orcId))
                .thenReturn(Optional.of(existente));
        when(notificacaoRepository.listarPorUserId(USER_ID)).thenReturn(List.of(existente));

        useCase.executar(USER_ID);

        verify(notificacaoRepository, never()).salvar(any());
        verify(notificacaoRepository).atualizar(existente);
    }

    @Test
    void notificacaoPersistidaComCondicaoResolvidaEhDeletada() {
        // Nenhum orcamento/meta no estado atual; mas ha uma notificacao persistida.
        UUID orcId = UUID.randomUUID();
        Notificacao orfa = new Notificacao(USER_ID, TipoNotificacao.ORCAMENTO_ATENCAO, orcId, "T", "D");
        when(notificacaoRepository.listarPorUserId(USER_ID)).thenReturn(List.of(orfa));

        useCase.executar(USER_ID);

        verify(notificacaoRepository).deletar(orfa.getId());
    }

    @Test
    void metaVencidaCriaNotificacaoMetaVencida() {
        UUID metaId = UUID.randomUUID();
        when(metaRepository.listar()).thenReturn(List.of(
                meta(metaId, "Viagem", LocalDate.now().minusDays(1), StatusMeta.EM_ANDAMENTO)));

        useCase.executar(USER_ID);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotificacao.META_VENCIDA);
        assertThat(captor.getValue().getReferenciaId()).isEqualTo(metaId);
    }

    @Test
    void metaConcluidaNaoGeraNotificacao() {
        when(metaRepository.listar()).thenReturn(List.of(
                meta(UUID.randomUUID(), "Feita", LocalDate.now().minusDays(1), StatusMeta.CONCLUIDA)));

        useCase.executar(USER_ID);

        verify(notificacaoRepository, never()).salvar(any());
    }

    @Test
    void orcamentoInativoNaoGeraNotificacao() {
        UUID orcId = UUID.randomUUID();
        when(orcamentoRepository.listar())
                .thenReturn(List.of(orcamento(orcId, UUID.randomUUID(), false)));

        useCase.executar(USER_ID);

        verify(calcularProgresso, never()).executar(eq(orcId));
        verify(notificacaoRepository, never()).salvar(any());
    }
}
