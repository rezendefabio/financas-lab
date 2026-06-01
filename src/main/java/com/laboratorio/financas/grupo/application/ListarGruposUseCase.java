package com.laboratorio.financas.grupo.application;

import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarGruposUseCase {

    private final GrupoRepository grupoRepository;

    public ListarGruposUseCase(GrupoRepository grupoRepository) {
        this.grupoRepository = grupoRepository;
    }

    @Transactional(readOnly = true)
    public List<Grupo> executar() {
        return grupoRepository.listarTodos();
    }
}
