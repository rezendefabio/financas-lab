package com.laboratorio.financas.emprestimo.application;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoNaoEncontradoException;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarEmprestimoPorIdUseCase {

    private final EmprestimoRepository repository;

    public BuscarEmprestimoPorIdUseCase(EmprestimoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Emprestimo executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EmprestimoNaoEncontradoException(id));
    }
}
