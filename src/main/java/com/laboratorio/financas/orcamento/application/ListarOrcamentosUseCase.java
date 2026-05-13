package com.laboratorio.financas.orcamento.application;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarOrcamentosUseCase {

    private final OrcamentoRepository orcamentoRepository;

    public ListarOrcamentosUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<Orcamento> executar() {
        return orcamentoRepository.listar();
    }
}
