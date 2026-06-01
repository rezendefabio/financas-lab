package com.laboratorio.financas.payee.application;

import com.laboratorio.financas.payee.domain.PayeeNaoEncontradoException;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarPayeeUseCase {

    private final PayeeRepository repository;

    public DeletarPayeeUseCase(PayeeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new PayeeNaoEncontradoException(id));
        repository.deleteById(id);
    }
}
