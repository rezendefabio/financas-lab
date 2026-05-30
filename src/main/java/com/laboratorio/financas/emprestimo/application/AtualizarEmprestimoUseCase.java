package com.laboratorio.financas.emprestimo.application;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoNaoEncontradoException;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import com.laboratorio.financas.shared.domain.Money;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarEmprestimoUseCase {

    private final EmprestimoRepository repository;

    public AtualizarEmprestimoUseCase(EmprestimoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Emprestimo executar(Comando comando) {
        Emprestimo emprestimo = repository.buscarPorId(comando.id())
                .orElseThrow(() -> new EmprestimoNaoEncontradoException(comando.id()));
        emprestimo.atualizar(
                comando.descricao(),
                comando.nomeTerceiro(),
                comando.tipo(),
                comando.valor(),
                comando.dataEmprestimo(),
                comando.quitado());
        return repository.atualizar(emprestimo);
    }

    public record Comando(UUID id, String descricao, String nomeTerceiro,
                          TipoEmprestimo tipo, Money valor, LocalDate dataEmprestimo,
                          boolean quitado) { }
}
