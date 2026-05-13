package com.laboratorio.financas.orcamento.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Orcamento {

    private final UUID id;
    private final String nome;
    private final Instant criadoEm;

    public Orcamento(String nome) {
        this(UUID.randomUUID(), nome, Instant.now());
    }

    public Orcamento(UUID id, String nome, Instant criadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        if (nome.isBlank()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        this.id = id;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Orcamento other)) {
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
        return "Orcamento{id=" + id + ", nome='" + nome + "'}";
    }
}
