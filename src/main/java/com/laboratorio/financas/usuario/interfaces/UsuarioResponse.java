package com.laboratorio.financas.usuario.interfaces;

import com.laboratorio.financas.usuario.domain.Usuario;
import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(UUID id, String email, String name, Instant criadoEm) {

    public static UsuarioResponse fromDomain(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getName(), u.getCriadoEm());
    }
}
