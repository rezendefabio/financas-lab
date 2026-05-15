package com.laboratorio.financas.usuario.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected UsuarioEntity() {
    }

    public UsuarioEntity(UUID id, String email, String senhaHash, boolean ativo, Instant criadoEm,
                         String name, Instant updatedAt) {
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
}
