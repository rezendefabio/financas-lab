package com.laboratorio.financas.lancamentorecorrente.application;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteNaoEncontradoException;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DesativarLancamentoRecorrenteUseCase {

    private final LancamentoRecorrenteRepository repository;

    public DesativarLancamentoRecorrenteUseCase(LancamentoRecorrenteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        LancamentoRecorrente lancamento = repository.buscarPorId(id)
                .orElseThrow(() -> new LancamentoRecorrenteNaoEncontradoException(id));
        lancamento.desativar();
        repository.atualizar(lancamento);
    }
}
