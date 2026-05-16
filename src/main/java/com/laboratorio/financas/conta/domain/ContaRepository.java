package com.laboratorio.financas.conta.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaRepository {

    Conta salvar(Conta conta);

    Optional<Conta> buscarPorId(UUID id);

    List<Conta> listarTodas();

    List<Conta> listarAtivas();

    void deletar(UUID id);
}
