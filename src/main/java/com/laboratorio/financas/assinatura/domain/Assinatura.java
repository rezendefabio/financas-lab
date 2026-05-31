package com.laboratorio.financas.assinatura.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Assinatura {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private String nome;
    private TipoAssinatura tipo;
    private Money valorMensal;
    private LocalDate dataRenovacao;
    private boolean ativa;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public Assinatura(UUID userId, String nome, TipoAssinatura tipo,
                      Money valorMensal, LocalDate dataRenovacao) {
        this(UUID.randomUUID(), userId, nome, tipo, valorMensal, dataRenovacao,
                true, Instant.now(), Instant.now());
    }

    public Assinatura(UUID id, UUID userId, String nome, TipoAssinatura tipo,
                      Money valorMensal, LocalDate dataRenovacao, boolean ativa,
                      Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        validarValorMensal(valorMensal);
        Objects.requireNonNull(dataRenovacao, "dataRenovacao nao pode ser nula");

        this.id = id;
        this.userId = userId;
        this.nome = nome.trim();
        this.tipo = tipo;
        this.valorMensal = valorMensal;
        this.dataRenovacao = dataRenovacao;
        this.ativa = ativa;
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

    private static void validarValorMensal(Money valorMensal) {
        Objects.requireNonNull(valorMensal, "valorMensal nao pode ser nulo");
        if (valorMensal.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valorMensal deve ser positivo");
        }
    }

    public void desativar() {
        this.ativa = false;
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(String novoNome, TipoAssinatura novoTipo,
                          Money novoValorMensal, LocalDate novaDataRenovacao, boolean novaAtiva) {
        validarNome(novoNome);
        Objects.requireNonNull(novoTipo, "tipo nao pode ser nulo");
        validarValorMensal(novoValorMensal);
        Objects.requireNonNull(novaDataRenovacao, "dataRenovacao nao pode ser nula");
        this.nome = novoNome.trim();
        this.tipo = novoTipo;
        this.valorMensal = novoValorMensal;
        this.dataRenovacao = novaDataRenovacao;
        this.ativa = novaAtiva;
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

    public TipoAssinatura getTipo() {
        return tipo;
    }

    public Money getValorMensal() {
        return valorMensal;
    }

    public LocalDate getDataRenovacao() {
        return dataRenovacao;
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
        if (!(o instanceof Assinatura other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
