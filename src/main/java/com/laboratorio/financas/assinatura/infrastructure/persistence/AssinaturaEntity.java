package com.laboratorio.financas.assinatura.infrastructure.persistence;

import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "assinatura")
public class AssinaturaEntity {

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
    private TipoAssinatura tipo;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor",
            column = @Column(name = "valor_mensal_valor", nullable = false, precision = 19, scale = 2)),
        @AttributeOverride(name = "moeda",
            column = @Column(name = "valor_mensal_moeda", nullable = false, length = 3))
    })
    private MoneyEmbeddable valorMensal;

    @NotNull
    @Column(name = "data_renovacao", nullable = false)
    private LocalDate dataRenovacao;

    @NotNull
    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected AssinaturaEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public AssinaturaEntity(UUID id, UUID userId, String nome, TipoAssinatura tipo,
                            MoneyEmbeddable valorMensal, LocalDate dataRenovacao, boolean ativa,
                            Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.tipo = tipo;
        this.valorMensal = valorMensal;
        this.dataRenovacao = dataRenovacao;
        this.ativa = ativa;
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

    public TipoAssinatura getTipo() {
        return tipo;
    }

    public MoneyEmbeddable getValorMensal() {
        return valorMensal;
    }

    public LocalDate getDataRenovacao() {
        return dataRenovacao;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
