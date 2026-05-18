package com.laboratorio.financas.anexo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnexoRepository {

    Anexo salvar(Anexo domain);

    Optional<Anexo> buscarPorId(UUID id);

    List<Anexo> listarTodos();
}
