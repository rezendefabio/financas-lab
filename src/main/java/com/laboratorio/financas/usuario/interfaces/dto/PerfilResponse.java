package com.laboratorio.financas.usuario.interfaces.dto;

import com.laboratorio.financas.usuario.domain.Usuario;
import java.time.Instant;
import java.util.UUID;

public record PerfilResponse(UUID id, String email, String name, Instant criadoEm, Instant updatedAt) {

    public static PerfilResponse fromDomain(Usuario u) {
        return new PerfilResponse(u.getId(), u.getEmail(), u.getName(), u.getCriadoEm(), u.getUpdatedAt());
    }
}
