package com.laboratorio.financas.payee.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayeeRepository {

    Optional<Payee> findById(UUID id);

    List<Payee> listarTodos();

    Payee save(Payee payee);

    void deleteById(UUID id);
}
