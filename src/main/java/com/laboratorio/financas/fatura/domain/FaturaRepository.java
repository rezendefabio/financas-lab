package com.laboratorio.financas.fatura.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaturaRepository {

    Fatura salvar(Fatura fatura);

    Optional<Fatura> buscarPorId(UUID id);

    List<Fatura> listarPorUserId(UUID userId);

    Fatura atualizar(Fatura fatura);

    void deletar(UUID id);
}
