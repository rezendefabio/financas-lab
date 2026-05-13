package com.laboratorio.financas.orcamento.application;

import com.laboratorio.financas.orcamento.domain.OrcamentoNaoEncontradoException;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DesativarOrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;

    public DesativarOrcamentoUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Transactional
    public void executar(UUID id) {
        var orcamento = orcamentoRepository.buscarPorId(id)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(id));
        orcamento.desativar();
        orcamentoRepository.atualizar(orcamento);
    }
}
