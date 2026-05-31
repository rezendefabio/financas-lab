package com.laboratorio.financas.lancamentorecorrente.infrastructure.persistence;

import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
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
@Table(name = "lancamento_recorrente")
public class LancamentoRecorrenteEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao;

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
    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(name = "categoria_id")
    private UUID categoriaId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "periodicidade", nullable = false, length = 20)
    private Periodicidade periodicidade;

    @NotNull
    @Column(name = "proxima_ocorrencia", nullable = false)
    private LocalDate proximaOcorrencia;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected LancamentoRecorrenteEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public LancamentoRecorrenteEntity(
            UUID id,
            UUID userId,
            String descricao,
            TipoTransacao tipo,
            MoneyEmbeddable valor,
            UUID contaId,
            UUID categoriaId,
            Periodicidade periodicidade,
            LocalDate proximaOcorrencia,
            boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.userId = userId;
        this.descricao = descricao;
        this.tipo = tipo;
        this.valor = valor;
        this.contaId = contaId;
        this.categoriaId = categoriaId;
        this.periodicidade = periodicidade;
        this.proximaOcorrencia = proximaOcorrencia;
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

    public String getDescricao() {
        return descricao;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public MoneyEmbeddable getValor() {
        return valor;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public LocalDate getProximaOcorrencia() {
        return proximaOcorrencia;
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
