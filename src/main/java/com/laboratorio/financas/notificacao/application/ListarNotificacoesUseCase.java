package com.laboratorio.financas.notificacao.application;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.NotificacaoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista as notificacoes ativas (nao-descartadas) do usuario.
 *
 * <p>Reconcilia primeiro (materializa o estado atual de orcamentos/metas,
 * preservando descartes) e entao retorna apenas as nao-descartadas. Como a
 * reconciliacao escreve, este use case NAO e readOnly.
 */
@Component
public class ListarNotificacoesUseCase {

    private final ReconciliarNotificacoesUseCase reconciliar;
    private final NotificacaoRepository repository;

    public ListarNotificacoesUseCase(ReconciliarNotificacoesUseCase reconciliar,
                                     NotificacaoRepository repository) {
        this.reconciliar = reconciliar;
        this.repository = repository;
    }

    @Transactional
    public List<Notificacao> executar(UUID userId) {
        reconciliar.executar(userId);
        return repository.listarPorUserId(userId).stream()
                .filter(n -> !n.isDescartada())
                .toList();
    }
}
