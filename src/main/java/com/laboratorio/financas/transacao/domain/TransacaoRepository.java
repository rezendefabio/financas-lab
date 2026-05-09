package com.laboratorio.financas.transacao.domain;

import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository {

    Transacao salvar(Transacao transacao);

    Optional<Transacao> buscarPorId(UUID id);

    void deletar(UUID id);
}
