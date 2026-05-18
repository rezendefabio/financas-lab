package com.laboratorio.financas.anexo.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Arquivo anexado a uma entidade do sistema.
 *
 * <p>A associacao com a entidade-dona e logica: {@code entidadeTipo} +
 * {@code entidadeId} (sem FK no banco) para nao acoplar bounded contexts.
 */
public final class Anexo {

    private final UUID id;
    private final String nome;
    private final String tipoConteudo;
    private final long tamanho;
    private final String chaveArmazenamento;
    private final String entidadeTipo;
    private final UUID entidadeId;
    private final Instant criadoEm;

    /**
     * Construtor de criacao. Gera {@code id}, {@code criadoEm} e a
     * {@code chaveArmazenamento} a partir dos demais campos.
     */
    public Anexo(String nome, String tipoConteudo, long tamanho, String entidadeTipo, UUID entidadeId) {
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        Objects.requireNonNull(tipoConteudo, "tipoConteudo nao pode ser nulo");
        Objects.requireNonNull(entidadeTipo, "entidadeTipo nao pode ser nulo");
        Objects.requireNonNull(entidadeId, "entidadeId nao pode ser nulo");
        if (nome.isBlank()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        if (tipoConteudo.isBlank()) {
            throw new IllegalArgumentException("tipoConteudo nao pode ser vazio");
        }
        if (entidadeTipo.isBlank()) {
            throw new IllegalArgumentException("entidadeTipo nao pode ser vazio");
        }
        if (tamanho <= 0) {
            throw new IllegalArgumentException("tamanho deve ser maior que zero");
        }
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.tipoConteudo = tipoConteudo;
        this.tamanho = tamanho;
        this.entidadeTipo = entidadeTipo;
        this.entidadeId = entidadeId;
        this.criadoEm = Instant.now();
        this.chaveArmazenamento = entidadeTipo.toLowerCase() + "/" + entidadeId + "/"
                + this.id + "." + extensaoDe(nome);
    }

    /**
     * Construtor de reconstrucao a partir da camada de persistencia.
     */
    public Anexo(UUID id, String nome, String tipoConteudo, long tamanho,
                 String chaveArmazenamento, String entidadeTipo, UUID entidadeId, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.tipoConteudo = tipoConteudo;
        this.tamanho = tamanho;
        this.chaveArmazenamento = chaveArmazenamento;
        this.entidadeTipo = entidadeTipo;
        this.entidadeId = entidadeId;
        this.criadoEm = criadoEm;
    }

    private static String extensaoDe(String nome) {
        int ponto = nome.lastIndexOf('.');
        if (ponto < 0 || ponto == nome.length() - 1) {
            return "bin";
        }
        String extensao = nome.substring(ponto + 1).toLowerCase();
        return extensao.isBlank() ? "bin" : extensao;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public long getTamanho() {
        return tamanho;
    }

    public String getChaveArmazenamento() {
        return chaveArmazenamento;
    }

    public String getEntidadeTipo() {
        return entidadeTipo;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Anexo other)) {
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
        return "Anexo{id=" + id + ", nome='" + nome + "', entidadeTipo='" + entidadeTipo
                + "', entidadeId=" + entidadeId + "}";
    }
}
