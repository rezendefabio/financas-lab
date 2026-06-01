package com.laboratorio.financas.emprestimo.application;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarEmprestimosUseCase {

    private final EmprestimoRepository repository;

    public ListarEmprestimosUseCase(EmprestimoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Emprestimo> executar() {
        return repository.listarTodos();
    }
}
