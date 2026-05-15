package com.laboratorio.financas.transacao.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final UUID categoriaId;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    // Campos novos da Fase 1
    private final UUID userId;
    private final StatusTransacao status;
    private final Instant deletedAt;
    private final UUID payeeId;
    private final UUID transferGroupId;
    private final UUID transferPairId;
    private final List<UUID> tagIds;

    /**
     * Par de transacoes gerado por uma transferencia entre contas.
     * A despesa e registrada na conta origem; a receita na conta destino.
     */
    public record TransferenciaPar(Transacao despesa, Transacao receita) { }

    /**
     * Construtor para criar nova Transacao simples (RECEITA ou DESPESA).
     * Gera id, define criadoEm=atualizadoEm=now, status=CLEARED, tagIds vazio.
     */
    public Transacao(
            TipoTransacao tipo,
            Money valor,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID categoriaId,
            UUID userId,
            StatusTransacao status,
            UUID payeeId,
            List<UUID> tagIds
    ) {
        this(
                UUID.randomUUID(),
                tipo,
                valor,
                data,
                descricao,
                contaId,
                categoriaId,
                Instant.now(),
                null,
                userId,
                status,
                null,
                payeeId,
                null,
                null,
                tagIds
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
            UUID categoriaId,
            Instant criadoEm,
            Instant atualizadoEm,
            UUID userId,
            StatusTransacao status,
            Instant deletedAt,
            UUID payeeId,
            UUID transferGroupId,
            UUID transferPairId,
            List<UUID> tagIds
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        Objects.requireNonNull(data, "data nao pode ser nula");
        Objects.requireNonNull(contaId, "contaId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarDescricao(descricao);
        validarValor(valor);

        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao.trim();
        this.contaId = contaId;
        this.categoriaId = categoriaId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = (atualizadoEm != null) ? atualizadoEm : criadoEm;
        this.userId = userId;
        this.status = (status != null) ? status : StatusTransacao.CLEARED;
        this.deletedAt = deletedAt;
        this.payeeId = payeeId;
        this.transferGroupId = transferGroupId;
        this.transferPairId = transferPairId;
        this.tagIds = (tagIds != null) ? Collections.unmodifiableList(new ArrayList<>(tagIds))
                : Collections.emptyList();
    }

    /**
     * Cria par de transacoes representando uma transferencia entre contas.
     * O modelo Fase 1 substitui o campo conta_destino_id por dois registros:
     * uma DESPESA na conta origem e uma RECEITA na conta destino,
     * ligados por transfer_group_id e transfer_pair_id cruzados.
     */
    public static TransferenciaPar criarParTransferencia(
            UUID userId,
            Money valor,
            UUID contaOrigemId,
            UUID contaDestinoId,
            LocalDate data,
            String descricao,
            UUID categoriaId
    ) {
        Objects.requireNonNull(contaOrigemId, "contaOrigemId nao pode ser nulo");
        Objects.requireNonNull(contaDestinoId, "contaDestinoId nao pode ser nulo");
        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException(
                    "TRANSFERENCIA nao pode ter contaOrigemId igual a contaDestinoId"
            );
        }

        UUID groupId = UUID.randomUUID();
        UUID idDespesa = UUID.randomUUID();
        UUID idReceita = UUID.randomUUID();
        Instant agora = Instant.now();

        Transacao despesa = new Transacao(
                idDespesa,
                TipoTransacao.DESPESA,
                valor,
                data,
                descricao,
                contaOrigemId,
                categoriaId,
                agora,
                agora,
                userId,
                StatusTransacao.CLEARED,
                null,
                null,
                groupId,
                idReceita,
                Collections.emptyList()
        );

        Transacao receita = new Transacao(
                idReceita,
                TipoTransacao.RECEITA,
                valor,
                data,
                descricao,
                contaDestinoId,
                categoriaId,
                agora,
                agora,
                userId,
                StatusTransacao.CLEARED,
                null,
                null,
                groupId,
                idDespesa,
                Collections.emptyList()
        );

        return new TransferenciaPar(despesa, receita);
    }

    /** Retorna true se esta transacao foi marcada como deletada (soft delete). */
    public boolean isDeleted() {
        return deletedAt != null;
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

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public UUID getUserId() {
        return userId;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getPayeeId() {
        return payeeId;
    }

    public UUID getTransferGroupId() {
        return transferGroupId;
    }

    public UUID getTransferPairId() {
        return transferPairId;
    }

    public List<UUID> getTagIds() {
        return tagIds;
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
