package com.laboratorio.financas.notificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.NotificacaoNaoEncontradaException;
import com.laboratorio.financas.notificacao.domain.NotificacaoRepository;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DescartarNotificacaoUseCaseTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private NotificacaoRepository repository;
    private DescartarNotificacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NotificacaoRepository.class);
        useCase = new DescartarNotificacaoUseCase(repository);
    }

    @Test
    void executarComIdExistenteDescartaEPersiste() {
        Notificacao n = new Notificacao(USER_ID, TipoNotificacao.META_VENCIDA, UUID.randomUUID(), "T", "D");
        when(repository.buscarPorId(n.getId())).thenReturn(Optional.of(n));
        when(repository.atualizar(any(Notificacao.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.executar(n.getId());

        assertThat(n.isDescartada()).isTrue();
        verify(repository).atualizar(n);
    }

    @Test
    void executarComIdInexistenteLancaNotificacaoNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(NotificacaoNaoEncontradaException.class);

        verify(repository, never()).atualizar(any());
    }
}
