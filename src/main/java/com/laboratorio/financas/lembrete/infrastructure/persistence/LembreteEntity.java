package com.laboratorio.financas.lembrete.infrastructure.persistence;

import com.laboratorio.financas.lembrete.domain.Prioridade;
import jakarta.persistence.Column;
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
@Table(name = "lembrete")
public class LembreteEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @NotNull
    @Column(name = "data_lembrete", nullable = false)
    private LocalDate dataLembrete;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 10)
    private Prioridade prioridade;

    @NotNull
    @Column(name = "concluido", nullable = false)
    private boolean concluido;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected LembreteEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public LembreteEntity(UUID id, UUID userId, String titulo, String descricao,
                          LocalDate dataLembrete, Prioridade prioridade, boolean concluido,
                          Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataLembrete = dataLembrete;
        this.prioridade = prioridade;
        this.concluido = concluido;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getDataLembrete() {
        return dataLembrete;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public boolean isConcluido() {
        return concluido;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
