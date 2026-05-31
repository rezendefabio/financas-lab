package com.laboratorio.financas.orcamento.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Orcamento {

    private final UUID id;
    private final UUID userId;
    private final UUID categoriaId;
    private final Money valorLimite;
    private LocalDate mesAno;
    private boolean ativo;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Orcamento(UUID userId, UUID categoriaId, Money valorLimite, LocalDate mesAno) {
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        if (categoriaId == null) {
            throw new IllegalArgumentException("categoriaId nao pode ser nulo");
        }
        if (valorLimite == null) {
            throw new IllegalArgumentException("valorLimite nao pode ser nulo");
        }
        if (valorLimite.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valorLimite deve ser maior que zero");
        }
        if (mesAno == null) {
            throw new IllegalArgumentException("mesAno nao pode ser nulo");
        }
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.categoriaId = categoriaId;
        this.valorLimite = valorLimite;
        this.mesAno = mesAno.getDayOfMonth() != 1 ? mesAno.withDayOfMonth(1) : mesAno;
        this.ativo = true;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public Orcamento(UUID id, UUID userId, UUID categoriaId, Money valorLimite, LocalDate mesAno,
                     boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        this.id = id;
        this.userId = userId;
        this.categoriaId = categoriaId;
        this.valorLimite = valorLimite;
        this.mesAno = mesAno;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public Money getValorLimite() {
        return valorLimite;
    }

    public LocalDate getMesAno() {
        return mesAno;
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
