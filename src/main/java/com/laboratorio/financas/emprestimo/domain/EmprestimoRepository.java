package com.laboratorio.financas.emprestimo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmprestimoRepository {

    Emprestimo salvar(Emprestimo emprestimo);

    Optional<Emprestimo> buscarPorId(UUID id);

    List<Emprestimo> listarPorUserId(UUID userId);

    Emprestimo atualizar(Emprestimo emprestimo);

    void deletar(UUID id);
}
