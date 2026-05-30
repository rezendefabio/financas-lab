package com.laboratorio.financas.lembrete.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Lembrete {

    private static final int TITULO_MAX_LENGTH = 100;
    private static final int DESCRICAO_MAX_LENGTH = 500;

    private final UUID id;
    private final UUID userId;
    private String titulo;
    private String descricao;
    private LocalDate dataLembrete;
    private Prioridade prioridade;
    private boolean concluido;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Lembrete(UUID userId, String titulo, String descricao,
                    LocalDate dataLembrete, Prioridade prioridade) {
        this(UUID.randomUUID(), userId, titulo, descricao, dataLembrete,
                prioridade, false, Instant.now(), Instant.now());
    }

    public Lembrete(UUID id, UUID userId, String titulo, String descricao,
                    LocalDate dataLembrete, Prioridade prioridade, boolean concluido,
                    Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        Objects.requireNonNull(atualizadoEm, "atualizadoEm nao pode ser nulo");
        validarTitulo(titulo);
        validarDescricao(descricao);
        Objects.requireNonNull(dataLembrete, "dataLembrete nao pode ser nulo");
        Objects.requireNonNull(prioridade, "prioridade nao pode ser nulo");

        this.id = id;
        this.userId = userId;
        this.titulo = titulo.trim();
        this.descricao = (descricao != null) ? descricao.trim() : null;
        this.dataLembrete = dataLembrete;
        this.prioridade = prioridade;
        this.concluido = concluido;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    private static void validarTitulo(String titulo) {
        Objects.requireNonNull(titulo, "titulo nao pode ser nulo");
        String trimmed = titulo.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("titulo nao pode ser vazio");
        }
        if (trimmed.length() > TITULO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "titulo nao pode ter mais de " + TITULO_MAX_LENGTH + " caracteres");
        }
    }

    private static void validarDescricao(String descricao) {
        if (descricao != null && descricao.length() > DESCRICAO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "descricao nao pode ter mais de " + DESCRICAO_MAX_LENGTH + " caracteres");
        }
    }

    public void atualizar(String novoTitulo, String novaDescricao,
                          LocalDate novaDataLembrete, Prioridade novaPrioridade,
                          boolean novoConcluido) {
        validarTitulo(novoTitulo);
        validarDescricao(novaDescricao);
        Objects.requireNonNull(novaDataLembrete, "dataLembrete nao pode ser nulo");
        Objects.requireNonNull(novaPrioridade, "prioridade nao pode ser nulo");

        this.titulo = novoTitulo.trim();
        this.descricao = (novaDescricao != null) ? novaDescricao.trim() : null;
        this.dataLembrete = novaDataLembrete;
        this.prioridade = novaPrioridade;
        this.concluido = novoConcluido;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Lembrete other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
