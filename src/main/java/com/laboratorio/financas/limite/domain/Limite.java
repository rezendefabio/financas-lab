package com.laboratorio.financas.limite.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Limite {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private String nome;
    private TipoLimite tipo;
    private Money valor;
    private boolean ativo;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    // Construtor de criacao: gera id e timestamps.
    public Limite(UUID userId, String nome, TipoLimite tipo, Money valor) {
        this(UUID.randomUUID(), userId, nome, tipo, valor, true, Instant.now(), Instant.now());
    }

    // Construtor de reconstituicao: todos os campos (para o mapper).
    public Limite(UUID id, UUID userId, String nome, TipoLimite tipo, Money valor,
                  boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);
        validarTipo(tipo);
        validarValor(valor);

        this.id = id;
        this.userId = userId;
        this.nome = nome.trim();
        this.tipo = tipo;
        this.valor = valor;
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
                "nome nao pode ter mais de " + NOME_MAX_LENGTH + " caracteres");
        }
    }

    private static void validarTipo(TipoLimite tipo) {
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
    }

    private static void validarValor(Money valor) {
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        if (valor.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
    }

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(String novoNome, TipoLimite novoTipo, Money novoValor) {
        validarNome(novoNome);
        validarTipo(novoTipo);
        validarValor(novoValor);
        this.nome = novoNome.trim();
        this.tipo = novoTipo;
        this.valor = novoValor;
        this.atualizadoEm = Instant.now();
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

    public TipoLimite getTipo() {
        return tipo;
    }

    public Money getValor() {
        return valor;
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
        if (!(o instanceof Limite other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
