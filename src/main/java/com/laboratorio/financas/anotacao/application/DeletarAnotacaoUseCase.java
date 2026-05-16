package com.laboratorio.financas.anotacao.application;

import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeletarAnotacaoUseCase {

    private final BuscarAnotacaoPorIdUseCase buscarUseCase;
    private final AnotacaoRepository repository;

    public DeletarAnotacaoUseCase(BuscarAnotacaoPorIdUseCase buscarUseCase,
                                  AnotacaoRepository repository) {
        this.buscarUseCase = buscarUseCase;
        this.repository = repository;
    }

    public void executar(UUID id) {
        buscarUseCase.executar(id);
        repository.deletar(id);
    }
}
