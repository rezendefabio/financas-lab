package com.laboratorio.financas.usuario.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Usuario {

    private final UUID id;
    private final String email;
    private final String senhaHash;
    private final boolean ativo;
    private final Instant criadoEm;
    private final String name;
    private final Instant updatedAt;

    public Usuario(String email, String senhaHash) {
        Objects.requireNonNull(email, "email nao pode ser nulo");
        Objects.requireNonNull(senhaHash, "senhaHash nao pode ser nulo");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email nao pode ser vazio");
        }
        this.id = UUID.randomUUID();
        this.email = email.toLowerCase().trim();
        this.senhaHash = senhaHash;
        this.ativo = true;
        this.criadoEm = Instant.now();
        this.name = null;
        this.updatedAt = Instant.now();
    }

    public Usuario(UUID id, String email, String senhaHash, boolean ativo, Instant criadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(email, "email nao pode ser nulo");
        Objects.requireNonNull(senhaHash, "senhaHash nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        this.id = id;
        this.email = email;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.name = null;
        this.updatedAt = null;
    }

    public Usuario(UUID id, String email, String senhaHash, boolean ativo, Instant criadoEm,
                   String name, Instant updatedAt) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(email, "email nao pode ser nulo");
        Objects.requireNonNull(senhaHash, "senhaHash nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        this.id = id;
        this.email = email;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getName() {
        return name;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", email=" + email + ", ativo=" + ativo + "}";
    }
}
