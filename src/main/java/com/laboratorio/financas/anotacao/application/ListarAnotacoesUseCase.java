package com.laboratorio.financas.anotacao.application;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListarAnotacoesUseCase {

    private final AnotacaoRepository repository;

    public ListarAnotacoesUseCase(AnotacaoRepository repository) {
        this.repository = repository;
    }

    public List<Anotacao> executar(UUID userId) {
        return repository.listarPorUsuario(userId);
    }
}
