package com.laboratorio.financas.meta.application;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarMetasUseCase {

    private final MetaRepository metaRepository;

    public ListarMetasUseCase(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Transactional(readOnly = true)
    public List<Meta> executar() {
        return metaRepository.listar();
    }
}
