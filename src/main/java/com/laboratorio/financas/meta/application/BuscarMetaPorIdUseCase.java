package com.laboratorio.financas.meta.application;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaNaoEncontradaException;
import com.laboratorio.financas.meta.domain.MetaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarMetaPorIdUseCase {

    private final MetaRepository metaRepository;

    public BuscarMetaPorIdUseCase(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Transactional(readOnly = true)
    public Meta executar(UUID id) {
        return metaRepository.buscarPorId(id)
                .orElseThrow(() -> new MetaNaoEncontradaException(id));
    }
}
