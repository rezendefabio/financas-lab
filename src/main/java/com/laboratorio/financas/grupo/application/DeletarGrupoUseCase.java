package com.laboratorio.financas.grupo.application;

import com.laboratorio.financas.grupo.domain.GrupoNaoEncontradoException;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarGrupoUseCase {

    private final GrupoRepository grupoRepository;

    public DeletarGrupoUseCase(GrupoRepository grupoRepository) {
        this.grupoRepository = grupoRepository;
    }

    @Transactional
    public void executar(UUID id, UUID userId) {
        grupoRepository.buscarPorIdEUserId(id, userId)
                .orElseThrow(() -> new GrupoNaoEncontradoException(id));
        grupoRepository.deletar(id);
    }
}
