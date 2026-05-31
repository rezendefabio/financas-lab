package com.laboratorio.financas.meta.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Meta {

    private final UUID id;
    private final UUID userId;
    private final String nome;
    private final Money valorAlvo;
    private Money valorAtual;
    private final LocalDate prazo;
    private StatusMeta status;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Meta(UUID userId, String nome, Money valorAlvo, LocalDate prazo) {
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome nao pode ser nulo ou vazio");
        }
        if (valorAlvo == null) {
            throw new IllegalArgumentException("valorAlvo nao pode ser nulo");
        }
        if (valorAlvo.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valorAlvo deve ser maior que zero");
        }
        if (prazo == null) {
            throw new IllegalArgumentException("prazo nao pode ser nulo");
        }
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.nome = nome;
        this.valorAlvo = valorAlvo;
        this.valorAtual = new Money(BigDecimal.ZERO, valorAlvo.moeda());
        this.prazo = prazo;
        this.status = StatusMeta.EM_ANDAMENTO;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public Meta(UUID id, UUID userId, String nome, Money valorAlvo, Money valorAtual, LocalDate prazo,
                StatusMeta status, Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.valorAlvo = valorAlvo;
        this.valorAtual = valorAtual;
        this.prazo = prazo;
        this.status = status;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public void registrarDeposito(Money deposito) {
        if (status != StatusMeta.EM_ANDAMENTO) {
            throw new IllegalStateException("Meta nao esta em andamento");
        }
        if (deposito == null || deposito.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("deposito deve ser nao nulo e maior que zero");
        }
        if (!deposito.moeda().equals(valorAlvo.moeda())) {
            throw new IllegalArgumentException("Moeda do deposito deve ser igual a moeda do valorAlvo");
        }
        this.valorAtual = new Money(valorAtual.valor().add(deposito.valor()), valorAtual.moeda());
        if (valorAtual.valor().compareTo(valorAlvo.valor()) >= 0) {
            this.status = StatusMeta.CONCLUIDA;
        }
        this.atualizadoEm = Instant.now();
    }

    public void cancelar() {
        if (status == StatusMeta.CONCLUIDA) {
            throw new IllegalStateException("Meta ja concluida nao pode ser cancelada");
        }
        this.status = StatusMeta.CANCELADA;
        this.atualizadoEm = Instant.now();
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

    public Money getValorAlvo() {
        return valorAlvo;
    }

    public Money getValorAtual() {
        return valorAtual;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public StatusMeta getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
