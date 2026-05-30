package com.laboratorio.financas.emprestimo.infrastructure.persistence;

import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
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
@Table(name = "emprestimo")
public class EmprestimoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "descricao", nullable = false, length = 100)
    private String descricao;

    @Column(name = "nome_terceiro", length = 100)
    private String nomeTerceiro;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoEmprestimo tipo;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor",
            column = @Column(name = "valor_valor", nullable = false, precision = 19, scale = 2)),
        @AttributeOverride(name = "moeda",
            column = @Column(name = "valor_moeda", nullable = false, length = 3))
    })
    private MoneyEmbeddable valor;

    @NotNull
    @Column(name = "data_emprestimo", nullable = false)
    private LocalDate dataEmprestimo;

    @NotNull
    @Column(name = "quitado", nullable = false)
    private boolean quitado;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected EmprestimoEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public EmprestimoEntity(UUID id, UUID userId, String descricao, String nomeTerceiro,
                            TipoEmprestimo tipo, MoneyEmbeddable valor, LocalDate dataEmprestimo,
                            boolean quitado, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.descricao = descricao;
        this.nomeTerceiro = nomeTerceiro;
        this.tipo = tipo;
        this.valor = valor;
        this.dataEmprestimo = dataEmprestimo;
        this.quitado = quitado;
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

    public String getNomeTerceiro() {
        return nomeTerceiro;
    }

    public TipoEmprestimo getTipo() {
        return tipo;
    }

    public MoneyEmbeddable getValor() {
        return valor;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public boolean isQuitado() {
        return quitado;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
