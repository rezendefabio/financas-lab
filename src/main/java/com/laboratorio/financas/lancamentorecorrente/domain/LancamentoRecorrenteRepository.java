package com.laboratorio.financas.lancamentorecorrente.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LancamentoRecorrenteRepository {

    LancamentoRecorrente salvar(LancamentoRecorrente lancamento);

    Optional<LancamentoRecorrente> buscarPorId(UUID id);

    List<LancamentoRecorrente> listar();

    LancamentoRecorrente atualizar(LancamentoRecorrente lancamento);
}
