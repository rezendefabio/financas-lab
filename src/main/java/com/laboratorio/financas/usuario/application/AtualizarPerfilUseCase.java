package com.laboratorio.financas.usuario.application;

import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarPerfilUseCase {

    private final UsuarioRepository usuarioRepository;

    public AtualizarPerfilUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public record Comando(String email, String novoNome) { }

    @Transactional
    public Usuario executar(Comando comando) {
        Usuario atual = usuarioRepository.buscarPorEmail(comando.email())
                .orElseThrow(() -> new IllegalStateException("Usuario nao encontrado"));
        String nome = (comando.novoNome() != null) ? comando.novoNome().trim() : null;
        Usuario atualizado = new Usuario(
                atual.getId(),
                atual.getEmail(),
                atual.getSenhaHash(),
                atual.isAtivo(),
                atual.getCriadoEm(),
                nome,
                Instant.now()
        );
        return usuarioRepository.atualizar(atualizado);
    }
}
