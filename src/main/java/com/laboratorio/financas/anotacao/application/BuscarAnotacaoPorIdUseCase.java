package com.laboratorio.financas.anotacao.application;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoNaoEncontradaException;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BuscarAnotacaoPorIdUseCase {

    private final AnotacaoRepository repository;

    public BuscarAnotacaoPorIdUseCase(AnotacaoRepository repository) {
        this.repository = repository;
    }

    public Anotacao executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(id));
    }
}
