package com.laboratorio.financas.instituicao.application;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.domain.InstituicaoNaoEncontradaException;
import com.laboratorio.financas.instituicao.domain.InstituicaoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarInstituicaoPorIdUseCase {

    private final InstituicaoRepository repository;

    public BuscarInstituicaoPorIdUseCase(InstituicaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Instituicao executar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new InstituicaoNaoEncontradaException(id));
    }
}
