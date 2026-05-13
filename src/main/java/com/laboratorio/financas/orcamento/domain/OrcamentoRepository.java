package com.laboratorio.financas.orcamento.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepository {

    Orcamento salvar(Orcamento orcamento);

    Optional<Orcamento> buscarPorId(UUID id);

    List<Orcamento> listar();

    Orcamento atualizar(Orcamento orcamento);
}
