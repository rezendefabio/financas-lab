package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarContasUseCase {

    private final ContaRepository repository;

    public ListarContasUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Conta> executar(boolean apenasAtivas) {
        if (apenasAtivas) {
            return repository.listarAtivas();
        }
        return repository.listarTodas();
    }
}
