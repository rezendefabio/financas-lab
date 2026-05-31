package com.laboratorio.financas.lancamentorecorrente.domain;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class LancamentoRecorrente {

    private static final int DESCRICAO_MAX_LENGTH = 200;

    private final UUID id;
    private final UUID userId;
    private final String descricao;
    private final TipoTransacao tipo;
    private final Money valor;
    private final UUID contaId;
    private final UUID categoriaId;
    private final Periodicidade periodicidade;
    private LocalDate proximaOcorrencia;
    private boolean ativo;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public LancamentoRecorrente(
            UUID userId,
            String descricao,
            TipoTransacao tipo,
            Money valor,
            UUID contaId,
            UUID categoriaId,
            Periodicidade periodicidade,
            LocalDate proximaOcorrencia
    ) {
        this(
                UUID.randomUUID(),
                userId,
                descricao,
                tipo,
                valor,
                contaId,
                categoriaId,
                periodicidade,
                proximaOcorrencia,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    public LancamentoRecorrente(
            UUID id,
            UUID userId,
            String descricao,
            TipoTransacao tipo,
            Money valor,
            UUID contaId,
            UUID categoriaId,
            Periodicidade periodicidade,
            LocalDate proximaOcorrencia,
            boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id nao pode ser nulo");
        }
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        if (descricao == null) {
            throw new IllegalArgumentException("descricao nao pode ser nula");
        }
        String trimmed = descricao.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("descricao nao pode ser vazia");
        }
        if (trimmed.length() > DESCRICAO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "descricao nao pode ter mais de " + DESCRICAO_MAX_LENGTH + " caracteres"
            );
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo nao pode ser nulo");
        }
        if (tipo == TipoTransacao.TRANSFERENCIA) {
            throw new IllegalArgumentException("TRANSFERENCIA nao e suportada em lancamentos recorrentes");
        }
        if (valor == null) {
            throw new IllegalArgumentException("valor nao pode ser nulo");
        }
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        if (contaId == null) {
            throw new IllegalArgumentException("contaId nao pode ser nulo");
        }
        if (periodicidade == null) {
            throw new IllegalArgumentException("periodicidade nao pode ser nula");
        }
        if (proximaOcorrencia == null) {
            throw new IllegalArgumentException("proximaOcorrencia nao pode ser nula");
        }

        this.id = id;
        this.userId = userId;
        this.descricao = trimmed;
        this.tipo = tipo;
        this.valor = valor;
        this.contaId = contaId;
        this.categoriaId = categoriaId;
        this.periodicidade = periodicidade;
        this.proximaOcorrencia = proximaOcorrencia;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public void avancarProximaOcorrencia() {
        this.proximaOcorrencia = periodicidade.calcularProxima(this.proximaOcorrencia);
        this.atualizadoEm = Instant.now();
    }

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public Money getValor() {
        return valor;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public LocalDate getProximaOcorrencia() {
        return proximaOcorrencia;
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
        if (!(o instanceof LancamentoRecorrente other)) {
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
        return "LancamentoRecorrente{id=" + id + ", descricao=" + descricao + ", tipo=" + tipo + "}";
    }
}
