package com.laboratorio.financas.instituicao.application;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.domain.InstituicaoRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarInstituicoesUseCase {

    private final InstituicaoRepository repository;

    public ListarInstituicoesUseCase(InstituicaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Instituicao> executar() {
        return repository.findAllAtivas();
    }
}
