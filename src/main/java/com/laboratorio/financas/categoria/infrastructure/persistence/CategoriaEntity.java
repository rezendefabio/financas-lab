package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.TipoCategoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categoria")
public class CategoriaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoCategoria tipo;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "categoria_pai_id")
    private UUID categoriaPaiId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "system", nullable = false)
    private boolean system;

    protected CategoriaEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public CategoriaEntity(
            UUID id,
            String nome,
            TipoCategoria tipo,
            Instant criadoEm,
            Instant atualizadoEm,
            UUID categoriaPaiId,
            UUID userId,
            boolean system
    ) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.categoriaPaiId = categoriaPaiId;
        this.userId = userId;
        this.system = system;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoCategoria getTipo() {
        return tipo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public UUID getCategoriaPaiId() {
        return categoriaPaiId;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isSystem() {
        return system;
    }
}
