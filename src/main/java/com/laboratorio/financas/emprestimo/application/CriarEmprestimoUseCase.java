package com.laboratorio.financas.emprestimo.application;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import com.laboratorio.financas.shared.domain.Money;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarEmprestimoUseCase {

    private final EmprestimoRepository repository;

    public CriarEmprestimoUseCase(EmprestimoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Emprestimo executar(Comando comando) {
        if (!comando.valor().ehPositivo()) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        Emprestimo emprestimo = new Emprestimo(
                comando.userId(),
                comando.descricao(),
                comando.nomeTerceiro(),
                comando.tipo(),
                comando.valor(),
                comando.dataEmprestimo()
        );
        return repository.salvar(emprestimo);
    }

    public record Comando(UUID userId,
                          String descricao,
                          String nomeTerceiro,
                          TipoEmprestimo tipo,
                          Money valor,
                          LocalDate dataEmprestimo) {
    }
}
