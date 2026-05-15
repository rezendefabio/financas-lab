package com.laboratorio.financas.tag.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Tag {

    private static final int NOME_MAX_LENGTH = 50;

    private final UUID id;
    private final UUID userId;
    private final String nome;
    private final String cor;
    private final Instant criadoEm;

    public Tag(UUID userId, String nome, String cor) {
        this(UUID.randomUUID(), userId, nome, cor, Instant.now());
    }

    public Tag(UUID id, UUID userId, String nome, String cor, Instant criadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.userId = userId;
        this.nome = nome.trim();
        this.cor = cor;
        this.criadoEm = criadoEm;
    }

    private static void validarNome(String nome) {
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        String trimmed = nome.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        if (trimmed.length() > NOME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "nome nao pode ter mais de " + NOME_MAX_LENGTH + " caracteres"
            );
        }
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

    public String getCor() {
        return cor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Tag{id=" + id + ", userId=" + userId + ", nome='" + nome + "'}";
    }
}
