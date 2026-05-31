package com.laboratorio.financas.notificacao.infrastructure.persistence;

import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
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
@Table(name = "notificacao")
public class NotificacaoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoNotificacao tipo;

    @NotNull
    @Column(name = "referencia_id", columnDefinition = "uuid", nullable = false)
    private UUID referenciaId;

    @NotNull
    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @NotNull
    @Column(name = "descricao", nullable = false, length = 300)
    private String descricao;

    @NotNull
    @Column(name = "descartada", nullable = false)
    private boolean descartada;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected NotificacaoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public NotificacaoEntity(UUID id, UUID userId, TipoNotificacao tipo, UUID referenciaId,
                             String titulo, String descricao, boolean descartada,
                             Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.descartada = descartada;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public TipoNotificacao getTipo() {
        return tipo;
    }

    public UUID getReferenciaId() {
        return referenciaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isDescartada() {
        return descartada;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
