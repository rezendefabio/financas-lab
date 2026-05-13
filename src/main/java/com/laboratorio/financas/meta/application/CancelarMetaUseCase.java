package com.laboratorio.financas.meta.application;

import com.laboratorio.financas.meta.domain.MetaNaoEncontradaException;
import com.laboratorio.financas.meta.domain.MetaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CancelarMetaUseCase {

    private final MetaRepository metaRepository;

    public CancelarMetaUseCase(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Transactional
    public void executar(UUID id) {
        var meta = metaRepository.buscarPorId(id)
                .orElseThrow(() -> new MetaNaoEncontradaException(id));
        meta.cancelar();
        metaRepository.atualizar(meta);
    }
}
