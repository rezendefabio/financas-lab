package com.laboratorio.financas.categoria.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Categoria {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final String nome;
    private final TipoCategoria tipo;
    private final UUID categoriaPaiId;
    private final UUID userId;
    private final boolean system;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    public Categoria(String nome, TipoCategoria tipo) {
        this(nome, tipo, null);
    }

    public Categoria(String nome, TipoCategoria tipo, UUID categoriaPaiId) {
        this(UUID.randomUUID(), nome, tipo, categoriaPaiId, null, false, Instant.now(), null);
    }

    public Categoria(String nome, TipoCategoria tipo, UUID categoriaPaiId, UUID userId) {
        this(UUID.randomUUID(), nome, tipo, categoriaPaiId, userId, false, Instant.now(), null);
    }

    public Categoria(
            UUID id,
            String nome,
            TipoCategoria tipo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this(id, nome, tipo, null, null, false, criadoEm, atualizadoEm);
    }

    public Categoria(
            UUID id,
            String nome,
            TipoCategoria tipo,
            UUID categoriaPaiId,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this(id, nome, tipo, categoriaPaiId, null, false, criadoEm, atualizadoEm);
    }

    public Categoria(
            UUID id,
            String nome,
            TipoCategoria tipo,
            UUID categoriaPaiId,
            UUID userId,
            boolean system,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.nome = nome.trim();
        this.tipo = tipo;
        this.categoriaPaiId = categoriaPaiId;
        this.userId = userId;
        this.system = system;
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

    public String getNome() {
        return nome;
    }

    public TipoCategoria getTipo() {
        return tipo;
    }

    public UUID getCategoriaPaiId() {
        return categoriaPaiId;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isSystem() {
        return system;
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
        if (!(o instanceof Categoria other)) {
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
        return "Categoria{id=" + id + ", nome='" + nome + "', tipo=" + tipo + ", system=" + system + "}";
    }
}
