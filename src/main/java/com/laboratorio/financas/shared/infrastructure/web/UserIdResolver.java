package com.laboratorio.financas.shared.infrastructure.web;

import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserIdResolver {

    private final UsuarioRepository usuarioRepository;

    public UserIdResolver(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UUID resolve(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado nao encontrado: " + email))
                .getId();
    }

    public UUID resolve() {
        return resolve(SecurityContextHolder.getContext().getAuthentication());
    }
}
