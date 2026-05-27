package com.laboratorio.financas.usuario.application;

import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import org.springframework.stereotype.Component;

@Component
public class BuscarPerfilUseCase {

    private final UsuarioRepository usuarioRepository;

    public BuscarPerfilUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario executar(String email) {
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario nao encontrado: " + email));
    }
}
