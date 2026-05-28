package com.laboratorio.financas.carteira.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Carteira {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private final UUID contaId;
    private String nome;
    private TipoCarteira tipo;
    private boolean ativo;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Carteira(UUID userId, UUID contaId, String nome, TipoCarteira tipo) {
        this(UUID.randomUUID(), userId, contaId, nome, tipo, true, Instant.now(), Instant.now());
    }

    public Carteira(
            UUID id,
            UUID userId,
            UUID contaId,
            String nome,
            TipoCarteira tipo,
            boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(contaId, "contaId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        Objects.requireNonNull(atualizadoEm, "atualizadoEm nao pode ser nulo");
        validarNome(nome);
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");

        this.id = id;
        this.userId = userId;
        this.contaId = contaId;
        this.nome = nome.trim();
        this.tipo = tipo;
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

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(String nome, TipoCarteira tipo) {
        validarNome(nome);
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        this.nome = nome.trim();
        this.tipo = tipo;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getNome() {
        return nome;
    }

    public TipoCarteira getTipo() {
        return tipo;
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
        if (!(o instanceof Carteira other)) {
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
        return "Carteira{id=" + id + ", userId=" + userId + ", nome='" + nome + "', tipo=" + tipo + "}";
    }
}
