package com.laboratorio.financas.orcamento.application;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarOrcamentoUseCase {

    private final OrcamentoRepository repository;

    public CriarOrcamentoUseCase(OrcamentoRepository repository) {
        this.repository = repository;
    }

    public record Comando(String nome) { }

    @Transactional
    public Orcamento executar(Comando comando) {
        Orcamento novo = new Orcamento(comando.nome());
        return repository.salvar(novo);
    }
}
