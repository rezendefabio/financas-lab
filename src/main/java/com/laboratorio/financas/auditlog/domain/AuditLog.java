package com.laboratorio.financas.auditlog.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro imutavel de um evento de auditoria.
 *
 * <p>Cada instancia representa uma unica mutacao (create, update ou delete)
 * sobre uma entidade do sistema. O campo {@code before} guarda o estado
 * anterior em JSON bruto (null em CREATE) e {@code after} o estado posterior
 * (null em DELETE). O dominio nao deserializa esse JSON -- apenas o transporta.
 */
public final class AuditLog {

    private final UUID id;
    private final String entityType;
    private final UUID entityId;
    private final AuditAction action;
    private final String userEmail;
    private final String screenCode;
    private final String before;
    private final String after;
    private final Instant criadoEm;

    public AuditLog(
            String entityType,
            UUID entityId,
            AuditAction action,
            String userEmail,
            String screenCode,
            String before,
            String after
    ) {
        this(UUID.randomUUID(), entityType, entityId, action, userEmail, screenCode, before, after, Instant.now());
    }

    public AuditLog(
            UUID id,
            String entityType,
            UUID entityId,
            AuditAction action,
            String userEmail,
            String screenCode,
            String before,
            String after,
            Instant criadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(entityType, "entityType nao pode ser nulo");
        if (entityType.isBlank()) {
            throw new IllegalArgumentException("entityType nao pode ser vazio");
        }
        Objects.requireNonNull(entityId, "entityId nao pode ser nulo");
        Objects.requireNonNull(action, "action nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userEmail = userEmail;
        this.screenCode = screenCode;
        this.before = before;
        this.after = after;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getScreenCode() {
        return screenCode;
    }

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditLog other)) {
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
        return "AuditLog{id=" + id
                + ", entityType='" + entityType + '\''
                + ", entityId=" + entityId
                + ", action=" + action + '}';
    }
}
