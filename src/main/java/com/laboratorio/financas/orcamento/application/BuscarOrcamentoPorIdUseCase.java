package com.laboratorio.financas.orcamento.application;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoNaoEncontradoException;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarOrcamentoPorIdUseCase {

    private final OrcamentoRepository orcamentoRepository;

    public BuscarOrcamentoPorIdUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Transactional(readOnly = true)
    public Orcamento executar(UUID id) {
        return orcamentoRepository.buscarPorId(id)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(id));
    }
}
