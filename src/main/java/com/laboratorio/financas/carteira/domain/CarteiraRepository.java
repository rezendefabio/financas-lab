package com.laboratorio.financas.carteira.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarteiraRepository {

    Carteira salvar(Carteira carteira);

    Optional<Carteira> buscarPorId(UUID id);

    List<Carteira> listarPorUserId(UUID userId);

    Carteira atualizar(Carteira carteira);

    void deletar(UUID id);
}
