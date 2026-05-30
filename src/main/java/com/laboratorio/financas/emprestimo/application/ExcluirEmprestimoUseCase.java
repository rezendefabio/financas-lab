package com.laboratorio.financas.emprestimo.application;

import com.laboratorio.financas.emprestimo.domain.EmprestimoNaoEncontradoException;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExcluirEmprestimoUseCase {

    private final EmprestimoRepository repository;

    public ExcluirEmprestimoUseCase(EmprestimoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new EmprestimoNaoEncontradoException(id));
        repository.deletar(id);
    }
}
