package com.laboratorio.financas.tag.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tag")
public class TagEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "nome", nullable = false, length = 50)
    private String nome;

    @Column(name = "cor", length = 7)
    private String cor;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected TagEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public TagEntity(UUID id, UUID userId, String nome, String cor, Instant criadoEm) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.cor = cor;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getNome() {
        return nome;
    }

    public String getCor() {
        return cor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
