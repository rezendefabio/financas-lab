package com.laboratorio.financas.notificacao.application;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.NotificacaoNaoEncontradaException;
import com.laboratorio.financas.notificacao.domain.NotificacaoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Descarta uma notificacao -- o descarte e PERSISTIDO (corrige o bug de a
 * notificacao reaparecer no proximo login).
 */
@Component
public class DescartarNotificacaoUseCase {

    private final NotificacaoRepository repository;

    public DescartarNotificacaoUseCase(NotificacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        Notificacao notificacao = repository.buscarPorId(id)
                .orElseThrow(() -> new NotificacaoNaoEncontradaException(id));
        notificacao.descartar();
        repository.atualizar(notificacao);
    }
}
