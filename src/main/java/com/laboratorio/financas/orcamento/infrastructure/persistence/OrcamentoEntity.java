package com.laboratorio.financas.orcamento.infrastructure.persistence;

import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "orcamento")
public class OrcamentoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "categoria_id", columnDefinition = "uuid", nullable = false)
    private UUID categoriaId;

    @NotNull
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "valor_limite_valor", nullable = false, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "valor_limite_moeda", nullable = false, length = 3))
    private MoneyEmbeddable valorLimite;

    @NotNull
    @Column(name = "mes_ano", nullable = false)
    private LocalDate mesAno;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected OrcamentoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public OrcamentoEntity(
            UUID id,
            UUID userId,
            UUID categoriaId,
            MoneyEmbeddable valorLimite,
            LocalDate mesAno,
            boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.userId = userId;
        this.categoriaId = categoriaId;
        this.valorLimite = valorLimite;
        this.mesAno = mesAno;
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

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public MoneyEmbeddable getValorLimite() {
        return valorLimite;
    }

    public LocalDate getMesAno() {
        return mesAno;
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
