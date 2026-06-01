package com.laboratorio.financas.anotacao.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class Anotacao {

    private final UUID id;
    private final UUID userId;
    private String titulo;
    private String conteudo;
    private TipoAnotacao tipo;
    private PrioridadeAnotacao prioridade;
    private Money valor;
    private LocalDate dataReferencia;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Anotacao(UUID userId, String titulo, String conteudo, TipoAnotacao tipo,
                    PrioridadeAnotacao prioridade, Money valor, LocalDate dataReferencia) {
        if (userId == null) {
            throw new IllegalArgumentException("userId nao pode ser nulo");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("titulo nao pode ser nulo ou vazio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo nao pode ser nulo");
        }
        if (prioridade == null) {
            throw new IllegalArgumentException("prioridade nao pode ser nula");
        }
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.valor = valor;
        this.dataReferencia = dataReferencia;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public Anotacao(UUID id, UUID userId, String titulo, String conteudo, TipoAnotacao tipo,
                    PrioridadeAnotacao prioridade, Money valor, LocalDate dataReferencia,
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

    public void atualizar(String titulo, String conteudo, TipoAnotacao tipo,
                          PrioridadeAnotacao prioridade, Money valor, LocalDate dataReferencia) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("titulo nao pode ser nulo ou vazio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo nao pode ser nulo");
        }
        if (prioridade == null) {
            throw new IllegalArgumentException("prioridade nao pode ser nula");
        }
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.valor = valor;
        this.dataReferencia = dataReferencia;
        this.atualizadoEm = Instant.now();
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

    public Money getValor() {
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
