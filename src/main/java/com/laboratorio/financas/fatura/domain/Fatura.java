package com.laboratorio.financas.fatura.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Fatura {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private final UUID contaId;
    private String nome;
    private LocalDate dataVencimento;
    private LocalDate dataFechamento;
    private Money valorTotal;
    private boolean paga;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Fatura(
            UUID userId,
            UUID contaId,
            String nome,
            LocalDate dataVencimento,
            LocalDate dataFechamento,
            Money valorTotal
    ) {
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(contaId, "contaId nao pode ser nulo");
        Objects.requireNonNull(dataVencimento, "dataVencimento nao pode ser nula");
        validarNome(nome);

        Instant agora = Instant.now();
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.contaId = contaId;
        this.nome = nome.trim();
        this.dataVencimento = dataVencimento;
        this.dataFechamento = dataFechamento;
        this.valorTotal = valorTotal;
        this.paga = false;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public Fatura(
            UUID id,
            UUID userId,
            UUID contaId,
            String nome,
            LocalDate dataVencimento,
            LocalDate dataFechamento,
            Money valorTotal,
            boolean paga,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(contaId, "contaId nao pode ser nulo");
        Objects.requireNonNull(dataVencimento, "dataVencimento nao pode ser nula");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.userId = userId;
        this.contaId = contaId;
        this.nome = nome.trim();
        this.dataVencimento = dataVencimento;
        this.dataFechamento = dataFechamento;
        this.valorTotal = valorTotal;
        this.paga = paga;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm != null ? atualizadoEm : criadoEm;
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

    public void pagar() {
        this.paga = true;
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(
            String nome,
            LocalDate dataVencimento,
            LocalDate dataFechamento,
            Money valorTotal
    ) {
        Objects.requireNonNull(dataVencimento, "dataVencimento nao pode ser nula");
        validarNome(nome);

        this.nome = nome.trim();
        this.dataVencimento = dataVencimento;
        this.dataFechamento = dataFechamento;
        this.valorTotal = valorTotal;
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

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public LocalDate getDataFechamento() {
        return dataFechamento;
    }

    public Money getValorTotal() {
        return valorTotal;
    }

    public boolean isPaga() {
        return paga;
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
        if (!(o instanceof Fatura other)) {
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
        return "Fatura{id=" + id + ", nome='" + nome + "', paga=" + paga + "}";
    }
}
