package com.laboratorio.financas.lancamentorecorrente.application;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ListarLancamentosRecorrentesUseCase {

    private final LancamentoRecorrenteRepository repository;

    public ListarLancamentosRecorrentesUseCase(LancamentoRecorrenteRepository repository) {
        this.repository = repository;
    }

    public List<LancamentoRecorrente> executar() {
        return repository.listar();
    }
}
