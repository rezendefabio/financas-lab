package com.laboratorio.financas.payee.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payee")
public class PayeeEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "categoria_padrao_id", columnDefinition = "uuid")
    private UUID categoriaPadraoId;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected PayeeEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public PayeeEntity(
            UUID id,
            UUID userId,
            String nome,
            UUID categoriaPadraoId,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.categoriaPadraoId = categoriaPadraoId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = Instant.now();
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

    public UUID getCategoriaPadraoId() {
        return categoriaPadraoId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
