package com.laboratorio.financas.instituicao.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Instituicao {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final String nome;
    private final String codigoBanco;
    private final TipoInstituicao tipo;
    private final String logoUrl;
    private final boolean ativa;
    private final Instant criadoEm;

    /**
     * Construtor para criar nova instituicao (id e criadoEm gerados pelo banco via seed/migration).
     * Usado apenas no contexto de testes ou criacao programatica.
     */
    public Instituicao(String nome, String codigoBanco, TipoInstituicao tipo, String logoUrl, boolean ativa) {
        this(UUID.randomUUID(), nome, codigoBanco, tipo, logoUrl, ativa, Instant.now());
    }

    /**
     * Construtor de reconstrucao do persistence.
     */
    public Instituicao(
            UUID id,
            String nome,
            String codigoBanco,
            TipoInstituicao tipo,
            String logoUrl,
            boolean ativa,
            Instant criadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.nome = nome.trim();
        this.codigoBanco = codigoBanco;
        this.tipo = tipo;
        this.logoUrl = logoUrl;
        this.ativa = ativa;
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

    public String getNome() {
        return nome;
    }

    public String getCodigoBanco() {
        return codigoBanco;
    }

    public TipoInstituicao getTipo() {
        return tipo;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Instituicao other)) {
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
        return "Instituicao{id=" + id + ", nome='" + nome + "', tipo=" + tipo + "}";
    }
}
