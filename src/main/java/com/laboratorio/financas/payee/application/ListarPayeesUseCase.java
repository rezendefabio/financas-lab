package com.laboratorio.financas.payee.application;

import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarPayeesUseCase {

    private final PayeeRepository repository;

    public ListarPayeesUseCase(PayeeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Payee> executar() {
        return repository.listarTodos();
    }
}
