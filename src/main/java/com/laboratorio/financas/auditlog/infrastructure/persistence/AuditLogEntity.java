package com.laboratorio.financas.auditlog.infrastructure.persistence;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @NotNull
    @Column(name = "entity_id", columnDefinition = "uuid", nullable = false)
    private UUID entityId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;

    @Column(name = "user_email", length = 200)
    private String userEmail;

    @Column(name = "screen_code", length = 20)
    private String screenCode;

    @Column(name = "before_state", columnDefinition = "text")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "text")
    private String afterState;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected AuditLogEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public AuditLogEntity(
            UUID id,
            String entityType,
            UUID entityId,
            AuditAction action,
            String userEmail,
            String screenCode,
            String beforeState,
            String afterState,
            Instant criadoEm
    ) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userEmail = userEmail;
        this.screenCode = screenCode;
        this.beforeState = beforeState;
        this.afterState = afterState;
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

    public String getBeforeState() {
        return beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
