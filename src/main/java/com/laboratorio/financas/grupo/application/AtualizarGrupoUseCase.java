package com.laboratorio.financas.grupo.application;

import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.grupo.domain.GrupoNaoEncontradoException;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarGrupoUseCase {

    private final GrupoRepository grupoRepository;

    public AtualizarGrupoUseCase(GrupoRepository grupoRepository) {
        this.grupoRepository = grupoRepository;
    }

    public record Comando(UUID id, String nome, String descricao) { }

    @Transactional
    public Grupo executar(Comando comando) {
        Grupo existente = grupoRepository.buscarPorId(comando.id())
                .orElseThrow(() -> new GrupoNaoEncontradoException(comando.id()));

        Grupo atualizado = existente.atualizar(comando.nome(), comando.descricao());
        return grupoRepository.salvar(atualizado);
    }
}
