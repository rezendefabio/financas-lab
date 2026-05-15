package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "transacao")
public class TransacaoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoTransacao tipo;

    @NotNull
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "valor_valor", nullable = false, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "valor_moeda", nullable = false, length = 3))
    private MoneyEmbeddable valor;

    @NotNull
    @Column(name = "data_transacao", nullable = false)
    private LocalDate data;

    @NotNull
    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao;

    @NotNull
    @Column(name = "conta_id", columnDefinition = "uuid", nullable = false)
    private UUID contaId;

    @Column(name = "categoria_id", columnDefinition = "uuid")
    private UUID categoriaId;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    // Campos novos da Fase 1
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusTransacao status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "payee_id", columnDefinition = "uuid")
    private UUID payeeId;

    @Column(name = "transfer_group_id", columnDefinition = "uuid")
    private UUID transferGroupId;

    @Column(name = "transfer_pair_id", columnDefinition = "uuid")
    private UUID transferPairId;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "transacao_tag", joinColumns = @JoinColumn(name = "transacao_id"))
    @Column(name = "tag_id")
    private Set<UUID> tagIds = new HashSet<>();

    protected TransacaoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public TransacaoEntity(
            UUID id,
            TipoTransacao tipo,
            MoneyEmbeddable valor,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID categoriaId,
            Instant criadoEm,
            Instant atualizadoEm,
            UUID userId,
            StatusTransacao status,
            Instant deletedAt,
            UUID payeeId,
            UUID transferGroupId,
            UUID transferPairId,
            Set<UUID> tagIds
    ) {
        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
        this.contaId = contaId;
        this.categoriaId = categoriaId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.userId = userId;
        this.status = status;
        this.deletedAt = deletedAt;
        this.payeeId = payeeId;
        this.transferGroupId = transferGroupId;
        this.transferPairId = transferPairId;
        this.tagIds = (tagIds != null) ? tagIds : new HashSet<>();
    }

    public UUID getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public MoneyEmbeddable getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public UUID getUserId() {
        return userId;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getPayeeId() {
        return payeeId;
    }

    public UUID getTransferGroupId() {
        return transferGroupId;
    }

    public UUID getTransferPairId() {
        return transferPairId;
    }

    public Set<UUID> getTagIds() {
        return java.util.Collections.unmodifiableSet(tagIds);
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
