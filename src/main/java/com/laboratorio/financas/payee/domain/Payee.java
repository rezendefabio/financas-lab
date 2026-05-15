package com.laboratorio.financas.payee.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payee {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private final String nome;
    private final UUID categoriaPadraoId;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    public Payee(UUID userId, String nome, UUID categoriaPadraoId) {
        this(UUID.randomUUID(), userId, nome, categoriaPadraoId, Instant.now(), null);
    }

    public Payee(
            UUID id,
            UUID userId,
            String nome,
            UUID categoriaPadraoId,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.userId = userId;
        this.nome = nome.trim();
        this.categoriaPadraoId = categoriaPadraoId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = (atualizadoEm != null) ? atualizadoEm : criadoEm;
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

    public UUID getCategoriaPadraoId() {
        return categoriaPadraoId;
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
        if (!(o instanceof Payee other)) {
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
        return "Payee{id=" + id + ", userId=" + userId + ", nome='" + nome + "'}";
    }
}
