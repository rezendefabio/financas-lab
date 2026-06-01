package com.laboratorio.financas.anotacao.infrastructure.persistence;

import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
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
@Table(name = "anotacao")
public class AnotacaoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "conteudo", columnDefinition = "TEXT")
    private String conteudo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoAnotacao tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 20)
    private PrioridadeAnotacao prioridade;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "valor_montante", nullable = true, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "valor_moeda", nullable = true, length = 3))
    private MoneyEmbeddable valor;

    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected AnotacaoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public AnotacaoEntity(UUID id, UUID userId, String titulo, String conteudo,
                          TipoAnotacao tipo, PrioridadeAnotacao prioridade,
                          MoneyEmbeddable valor, LocalDate dataReferencia,
                          Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.valor = valor;
        this.dataReferencia = dataReferencia;
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

    public String getConteudo() {
        return conteudo;
    }

    public TipoAnotacao getTipo() {
        return tipo;
    }

    public PrioridadeAnotacao getPrioridade() {
        return prioridade;
    }

    public MoneyEmbeddable getValor() {
        return valor;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
