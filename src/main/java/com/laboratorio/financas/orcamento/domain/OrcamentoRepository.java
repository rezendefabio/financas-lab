package com.laboratorio.financas.orcamento.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepository {

    Orcamento salvar(Orcamento domain);

    Optional<Orcamento> buscarPorId(UUID id);

    List<Orcamento> listarTodos();
}
