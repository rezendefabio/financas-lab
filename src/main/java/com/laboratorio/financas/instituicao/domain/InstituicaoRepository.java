package com.laboratorio.financas.instituicao.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstituicaoRepository {

    Optional<Instituicao> findById(UUID id);

    List<Instituicao> findAll();

    List<Instituicao> findAllAtivas();

    Instituicao save(Instituicao instituicao);
}
