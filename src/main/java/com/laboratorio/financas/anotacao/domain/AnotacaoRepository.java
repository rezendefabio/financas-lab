package com.laboratorio.financas.anotacao.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnotacaoRepository {

    Anotacao salvar(Anotacao anotacao);

    Optional<Anotacao> buscarPorId(UUID id);

    List<Anotacao> listarPorUsuario(UUID userId);

    void deletar(UUID id);
}
