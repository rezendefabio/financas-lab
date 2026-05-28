package com.laboratorio.financas.grupo.application;

import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarGrupoUseCase {

    private final GrupoRepository grupoRepository;

    public CriarGrupoUseCase(GrupoRepository grupoRepository) {
        this.grupoRepository = grupoRepository;
    }

    public record Comando(UUID userId, String nome, String descricao) { }

    @Transactional
    public Grupo executar(Comando comando) {
        Grupo grupo = new Grupo(comando.userId(), comando.nome(), comando.descricao());
        return grupoRepository.salvar(grupo);
    }
}
