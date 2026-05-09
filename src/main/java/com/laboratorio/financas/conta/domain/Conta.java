package com.laboratorio.financas.conta.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Conta {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final String nome;
    private final TipoConta tipo;
    private final Money saldoInicial;
    private final boolean ativa;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    /**
     * Construtor para criar nova Conta. Gera id, define ativa=true, criadoEm=atualizadoEm=now.
     */
    public Conta(String nome, TipoConta tipo, Money saldoInicial) {
        this(
                UUID.randomUUID(),
                nome,
                tipo,
                saldoInicial,
                true,
                Instant.now(),
                null  // atualizadoEm sera ajustado para criadoEm pelo construtor de reconstrucao
        );
    }

    /**
     * Construtor de reconstrucao. Usado pelo repository para hidratar instancia persistida.
     * Todos os campos sao recebidos e validados. atualizadoEm aceita null (defaults para criadoEm).
     */
    public Conta(
            UUID id,
            String nome,
            TipoConta tipo,
            Money saldoInicial,
            boolean ativa,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(saldoInicial, "saldoInicial nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.nome = nome.trim();
        this.tipo = tipo;
        this.saldoInicial = saldoInicial;
        this.ativa = ativa;
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

    public Conta desativar() {
        if (!this.ativa) {
            return this;
        }
        return new Conta(
                this.id,
                this.nome,
                this.tipo,
                this.saldoInicial,
                false,
                this.criadoEm,
                Instant.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public Money getSaldoInicial() {
        return saldoInicial;
    }

    public boolean isAtiva() {
        return ativa;
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
        if (!(o instanceof Conta other)) {
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
        return "Conta{id=" + id + ", nome='" + nome + "', tipo=" + tipo + ", ativa=" + ativa + "}";
    }
}
