package com.laboratorio.financas.lancamentorecorrente.application;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteNaoEncontradoException;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BuscarLancamentoRecorrentePorIdUseCase {

    private final LancamentoRecorrenteRepository repository;

    public BuscarLancamentoRecorrentePorIdUseCase(LancamentoRecorrenteRepository repository) {
        this.repository = repository;
    }

    public LancamentoRecorrente executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new LancamentoRecorrenteNaoEncontradoException(id));
    }
}
