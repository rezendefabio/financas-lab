package com.laboratorio.financas.grupo.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Grupo {

    private static final int NOME_MAX_LENGTH = 100;
    private static final int DESCRICAO_MAX_LENGTH = 300;

    private final UUID id;
    private final UUID userId;
    private final String nome;
    private final String descricao;
    private final boolean ativo;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    public Grupo(UUID userId, String nome, String descricao) {
        this(UUID.randomUUID(), userId, nome, descricao, true, Instant.now(), Instant.now());
    }

    public Grupo(
            UUID id,
            UUID userId,
            String nome,
            String descricao,
            boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        Objects.requireNonNull(atualizadoEm, "atualizadoEm nao pode ser nulo");
        validarNome(nome);
        validarDescricao(descricao);

        this.id = id;
        this.userId = userId;
        this.nome = nome.trim();
        this.descricao = descricao;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
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

    private static void validarDescricao(String descricao) {
        if (descricao != null && descricao.length() > DESCRICAO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "descricao nao pode ter mais de " + DESCRICAO_MAX_LENGTH + " caracteres"
            );
        }
    }

    public Grupo atualizar(String nome, String descricao) {
        return new Grupo(
                this.id,
                this.userId,
                nome,
                descricao,
                this.ativo,
                this.criadoEm,
                Instant.now()
        );
    }

    public Grupo desativar() {
        return new Grupo(
                this.id,
                this.userId,
                this.nome,
                this.descricao,
                false,
                this.criadoEm,
                Instant.now()
        );
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

    public String getDescricao() {
        return descricao;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Grupo other)) {
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
        return "Grupo{id=" + id + ", userId=" + userId + ", nome='" + nome + "'}";
    }
}
