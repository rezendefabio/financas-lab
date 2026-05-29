package com.laboratorio.financas.limite.infrastructure.persistence;

import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "limite")
public class LimiteEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoLimite tipo;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor",
            column = @Column(name = "valor", nullable = false, precision = 19, scale = 2)),
        @AttributeOverride(name = "moeda",
            column = @Column(name = "moeda", nullable = false, length = 3))
    })
    private MoneyEmbeddable valor;

    @NotNull
    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected LimiteEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public LimiteEntity(UUID id, UUID userId, String nome, TipoLimite tipo, MoneyEmbeddable valor,
                        boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.tipo = tipo;
        this.valor = valor;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
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

    public TipoLimite getTipo() {
        return tipo;
    }

    public MoneyEmbeddable getValor() {
        return valor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
