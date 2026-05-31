package com.laboratorio.financas.notificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.NotificacaoRepository;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarNotificacoesUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private ReconciliarNotificacoesUseCase reconciliar;
    private NotificacaoRepository repository;
    private ListarNotificacoesUseCase useCase;

    @BeforeEach
    void setUp() {
        reconciliar = Mockito.mock(ReconciliarNotificacoesUseCase.class);
        repository = Mockito.mock(NotificacaoRepository.class);
        useCase = new ListarNotificacoesUseCase(reconciliar, repository);
    }

    @Test
    void executarReconciliaAntesDeListar() {
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of());

        useCase.executar(USER_ID);

        verify(reconciliar).executar(USER_ID);
        verify(repository).listarPorUserId(USER_ID);
    }

    @Test
    void executarFiltraDescartadas() {
        Notificacao ativa = new Notificacao(USER_ID, TipoNotificacao.ORCAMENTO_EXCEDIDO, UUID.randomUUID(), "Ativa", "D");
        Notificacao descartada = new Notificacao(USER_ID, TipoNotificacao.META_VENCIDA, UUID.randomUUID(), "Oculta", "D");
        descartada.descartar();
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(ativa, descartada));

        List<Notificacao> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitulo()).isEqualTo("Ativa");
    }
}
