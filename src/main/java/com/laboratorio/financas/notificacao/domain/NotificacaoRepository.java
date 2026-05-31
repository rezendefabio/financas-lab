package com.laboratorio.financas.notificacao.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacaoRepository {

    Notificacao salvar(Notificacao notificacao);

    Optional<Notificacao> buscarPorId(UUID id);

    /** Todas as notificacoes do usuario (descartadas ou nao) -- usada na reconciliacao. */
    List<Notificacao> listarPorUserId(UUID userId);

    /** Busca pela chave natural, para o upsert da reconciliacao. */
    Optional<Notificacao> buscarPorChaveNatural(UUID userId, TipoNotificacao tipo, UUID referenciaId);

    Notificacao atualizar(Notificacao notificacao);

    void deletar(UUID id);
}
