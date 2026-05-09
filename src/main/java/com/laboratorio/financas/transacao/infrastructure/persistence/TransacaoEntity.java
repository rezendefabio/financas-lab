package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import jakarta.persistence.AttributeOverride;
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

    @Column(name = "conta_destino_id", columnDefinition = "uuid")
    private UUID contaDestinoId;

    @Column(name = "categoria_id", columnDefinition = "uuid")
    private UUID categoriaId;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

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
            UUID contaDestinoId,
            UUID categoriaId,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
        this.contaId = contaId;
        this.contaDestinoId = contaDestinoId;
        this.categoriaId = categoriaId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
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

    public UUID getContaDestinoId() {
        return contaDestinoId;
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
}
