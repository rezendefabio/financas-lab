package com.laboratorio.financas.conta.infrastructure.persistence;

import com.laboratorio.financas.conta.domain.TipoConta;
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
import java.util.UUID;

@Entity
@Table(name = "conta")
public class ContaEntity {

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
    private TipoConta tipo;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor", column = @Column(name = "saldo_inicial_valor", nullable = false, precision = 19, scale = 2)),
        @AttributeOverride(name = "moeda", column = @Column(name = "saldo_inicial_moeda", nullable = false, length = 3))
    })
    private MoneyEmbeddable saldoInicial;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor", column = @Column(name = "saldo_atual_valor", precision = 15, scale = 2)),
        @AttributeOverride(name = "moeda", column = @Column(name = "saldo_atual_moeda", length = 3))
    })
    private MoneyEmbeddable saldoAtual;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "valor", column = @Column(name = "limite_credito_valor", precision = 15, scale = 2)),
        @AttributeOverride(name = "moeda", column = @Column(name = "limite_credito_moeda", length = 3))
    })
    private MoneyEmbeddable limiteCredito;

    @Column(name = "dia_fechamento")
    private Integer diaFechamento;

    @Column(name = "dia_vencimento")
    private Integer diaVencimento;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ContaEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public ContaEntity(
            UUID id,
            UUID userId,
            String nome,
            TipoConta tipo,
            MoneyEmbeddable saldoInicial,
            MoneyEmbeddable saldoAtual,
            MoneyEmbeddable limiteCredito,
            Integer diaFechamento,
            Integer diaVencimento,
            boolean ativa,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.tipo = tipo;
        this.saldoInicial = saldoInicial;
        this.saldoAtual = saldoAtual;
        this.limiteCredito = limiteCredito;
        this.diaFechamento = diaFechamento;
        this.diaVencimento = diaVencimento;
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

    public TipoConta getTipo() {
        return tipo;
    }

    public MoneyEmbeddable getSaldoInicial() {
        return saldoInicial;
    }

    public MoneyEmbeddable getSaldoAtual() {
        return saldoAtual;
    }

    public MoneyEmbeddable getLimiteCredito() {
        return limiteCredito;
    }

    public Integer getDiaFechamento() {
        return diaFechamento;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
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
