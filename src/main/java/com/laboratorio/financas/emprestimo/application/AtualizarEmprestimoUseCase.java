package com.laboratorio.financas.emprestimo.application;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoNaoEncontradoException;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarEmprestimoUseCase {

    private final EmprestimoRepository repository;

    public AtualizarEmprestimoUseCase(EmprestimoRepository repository) {
        this.repository = repository;
    }

    public record Comando(
            UUID id,
            String descricao,
            String nomeTerceiro,
            TipoEmprestimo tipo,
            BigDecimal valor,
            String moeda,
            LocalDate dataEmprestimo,
            boolean quitado) { }

    @Transactional
    public Emprestimo executar(Comando comando) {
        Emprestimo entidade = repository.buscarPorId(comando.id())
                .orElseThrow(() -> new EmprestimoNaoEncontradoException(comando.id()));
        Money valor = new Money(comando.valor(), Currency.getInstance(comando.moeda()));
        entidade.atualizar(
                comando.descricao(),
                comando.nomeTerceiro(),
                comando.tipo(),
                valor,
                comando.dataEmprestimo(),
                comando.quitado());
        return repository.atualizar(entidade);
    }
}
