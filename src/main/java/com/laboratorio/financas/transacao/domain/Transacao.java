package com.laboratorio.financas.transacao.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Transacao {

    private static final int DESCRICAO_MAX_LENGTH = 200;

    private final UUID id;
    private final TipoTransacao tipo;
    private final Money valor;
    private final LocalDate data;
    private final String descricao;
    private final UUID contaId;
    private final UUID contaDestinoId;
    private final UUID categoriaId;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    /**
     * Construtor para criar nova Transacao. Gera id, define criadoEm=atualizadoEm=now.
     */
    public Transacao(
            TipoTransacao tipo,
            Money valor,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId
    ) {
        this(
                UUID.randomUUID(),
                tipo,
                valor,
                data,
                descricao,
                contaId,
                contaDestinoId,
                categoriaId,
                Instant.now(),
                null
        );
    }

    /**
     * Construtor de reconstrucao. Usado pelo repository para hidratar instancia persistida.
     */
    public Transacao(
            UUID id,
            TipoTransacao tipo,
            Money valor,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        Objects.requireNonNull(data, "data nao pode ser nula");
        Objects.requireNonNull(contaId, "contaId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarDescricao(descricao);
        validarValor(valor);
        validarRegrasDeTransferencia(tipo, contaId, contaDestinoId, categoriaId);

        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao.trim();
        this.contaId = contaId;
        this.contaDestinoId = contaDestinoId;
        this.categoriaId = categoriaId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = (atualizadoEm != null) ? atualizadoEm : criadoEm;
    }

    private static void validarDescricao(String descricao) {
        Objects.requireNonNull(descricao, "descricao nao pode ser nula");
        String trimmed = descricao.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("descricao nao pode ser vazia");
        }
        if (trimmed.length() > DESCRICAO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "descricao nao pode ter mais de " + DESCRICAO_MAX_LENGTH + " caracteres"
            );
        }
    }

    private static void validarValor(Money valor) {
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
    }

    private static void validarRegrasDeTransferencia(
            TipoTransacao tipo,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId
    ) {
        if (tipo == TipoTransacao.TRANSFERENCIA) {
            if (contaDestinoId == null) {
                throw new IllegalArgumentException(
                        "TRANSFERENCIA exige contaDestinoId"
                );
            }
            if (contaId.equals(contaDestinoId)) {
                throw new IllegalArgumentException(
                        "TRANSFERENCIA nao pode ter contaId igual a contaDestinoId"
                );
            }
            if (categoriaId != null) {
                throw new IllegalArgumentException(
                        "TRANSFERENCIA nao deve ter categoriaId"
                );
            }
        } else {
            if (contaDestinoId != null) {
                throw new IllegalArgumentException(
                        tipo + " nao deve ter contaDestinoId"
                );
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public Money getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getContaDestinoId() {
        return contaDestinoId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
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
        if (!(o instanceof Transacao other)) {
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
        return "Transacao{id=" + id + ", tipo=" + tipo + ", valor=" + valor + ", data=" + data + "}";
    }
}
