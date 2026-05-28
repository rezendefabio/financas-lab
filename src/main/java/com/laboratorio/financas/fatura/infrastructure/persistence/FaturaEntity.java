package com.laboratorio.financas.fatura.infrastructure.persistence;

import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
@Table(name = "fatura")
public class FaturaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "conta_id", columnDefinition = "uuid", nullable = false)
    private UUID contaId;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_fechamento")
    private LocalDate dataFechamento;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor", column = @Column(name = "valor_total_valor", nullable = true, precision = 19, scale = 2)),
        @AttributeOverride(name = "moeda", column = @Column(name = "valor_total_moeda", nullable = true, length = 3))
    })
    private MoneyEmbeddable valorTotal;

    @Column(name = "paga", nullable = false)
    private boolean paga;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected FaturaEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public FaturaEntity(
            UUID id,
            UUID userId,
            UUID contaId,
            String nome,
            LocalDate dataVencimento,
            LocalDate dataFechamento,
            MoneyEmbeddable valorTotal,
            boolean paga,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.userId = userId;
        this.contaId = contaId;
        this.nome = nome;
        this.dataVencimento = dataVencimento;
        this.dataFechamento = dataFechamento;
        this.valorTotal = valorTotal;
        this.paga = paga;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getNome() {
        return nome;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public LocalDate getDataFechamento() {
        return dataFechamento;
    }

    public MoneyEmbeddable getValorTotal() {
        return valorTotal;
    }

    public boolean isPaga() {
        return paga;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
