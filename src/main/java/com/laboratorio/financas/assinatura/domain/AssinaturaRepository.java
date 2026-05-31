package com.laboratorio.financas.assinatura.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssinaturaRepository {

    Assinatura salvar(Assinatura entidade);

    Optional<Assinatura> buscarPorId(UUID id);

    List<Assinatura> listarPorUserId(UUID userId);

    Assinatura atualizar(Assinatura entidade);

    void deletar(UUID id);
}
